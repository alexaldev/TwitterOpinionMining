package sentimentAnalysis;

import com.mongodb.Block;
import com.mongodb.MongoClient;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.DistinctIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Projections;
import domain.TweetModel;
import org.bson.BsonInt64;
import org.bson.Document;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.DefaultXYDataset;
import repository.MongoRepository;
import twitter4j.TwitterException;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import static com.mongodb.client.model.Filters.eq;

public class UserSentimentAnalysis extends SentimentAnalysis {

    public UserSentimentAnalysis(MongoRepository repo) {
        super(repo);
    }

    /**
     * Calculates average sentiment scores for each user in collection
     * @return iterable with users
     */
    public AggregateIterable<TweetModel> calculateTotalUsersSentimentScore() {
        return repo.collectionAggregate(Arrays.asList(
                // Aggregation: GROUPBY userID, AVG(pos, neg, neut)
                Aggregates.group("$userID",
                        Accumulators.avg("positiveProbability", "$positiveProbability"),
                        Accumulators.avg("negativeProbability", "$negativeProbability"),
                        Accumulators.avg("neutralProbability", "$neutralProbability")),
                // Projection: Rename _id to userID, in  order to fit to TweetModel Class
                Aggregates.project(Projections.fields(
                        Projections.excludeId(),
                        Projections.computed("userID", "$_id"),
                        Projections.include("positiveProbability", "negativeProbability", "neutralProbability")))
                )
        );
    }

    /**
     * Creates a collection with name as repo's collection name with suffix '_usersFeelings', in which it stores user
     * sentiment scores as they are calculated from {@link #calculateTotalUsersSentimentScore()}
     */
    public void storeUsersSentimentScore() {
        AggregateIterable<TweetModel> usersScore = calculateTotalUsersSentimentScore();

        // put users in a list
        List<Document> usersList = new ArrayList<>();
        usersScore.forEach((Block<? super TweetModel>) (TweetModel tweetModel) ->
                usersList.add(
                        new Document("userID", tweetModel.getUserID())
                                .append("avgPositiveProbability", tweetModel.getPositiveProbability())
                                .append("avgNegativeProbability", tweetModel.getNegativeProbability())
                                .append("avgNeutralProbability", tweetModel.getNeutralProbability())
                ));

        // Create new collection
        MongoCollection<Document> collection = new MongoClient(repo.getHostname(), repo.getPort())
                .getDatabase(repo.getDatabaseName())
                .getCollection(repo.getCollectionName() + "_usersFeelings");

        // Add users to collection. If collection already exists, ask for drop or not.
        if (collection.count() == 0)
            collection.insertMany(usersList);
        else {
            System.out.print(repo.getCollectionName() + "_usersFeelings already exists. Do you want to drop it and fill it again? (y/N): ");
            String ans = new Scanner(System.in).nextLine();
            if (ans.toLowerCase().equals("y")) {
                collection.drop();
                collection.insertMany(usersList);
                System.out.println("Collection has been dropped and recreated");
            } else
                System.out.println("No changes were made in collection");
        }

    }

    /**
     * Calculates Followers-Friends ratio for each user in collection
     * @param userID twitter user ID
     * @return ff ratio, unless friends count equals 0, then it returns -1
     * @throws TwitterException
     */
    public double calculateFollowersFriendsRatio(long userID) throws TwitterException {

        // get all tweets from this user
        FindIterable<TweetModel> it = repo.getCollectionIterable(eq("userID", userID));

        // keep only the first one
        TweetModel tweetModel = it.first();

        // Get counts
        int followers = tweetModel.getUserFollowersCount();
        int friends = tweetModel.getUserFriendsCount();

        // Return ratio
        if (friends == 0)
            return -1;

        return ((double) followers) / friends;
    }

    /**
     * Produces cdf of Followers-Friends Ratio for this collection
     * @param chartsDirectory Directory in which plot will be saved
     */
    public void produceCumulativeDistributionFrequency(String chartsDirectory) {

        // Get an iterable with distinct userIDs from collection
        DistinctIterable<BsonInt64> dit = repo.distinctCollection("userID", BsonInt64.class);

        // Calculate cumulative followers-friends ratio for all users
        List<Double> ffRatioCumm = new ArrayList<>();
        ffRatioCumm.add(0.0);
        dit.forEach((Block<? super BsonInt64>) (BsonInt64 userID) -> {
            try {
                double ratio = calculateFollowersFriendsRatio(userID.getValue());
                if (ratio != -1)
                    ffRatioCumm.add(ffRatioCumm.get(ffRatioCumm.size()-1) + ratio); // add value of previous + value of this
            } catch (TwitterException e) {
                if (e.getStatusCode() == 403 || e.getStatusCode() == 404)
                    System.err.println("User " + userID.longValue() + " does not exist or have been removed");
                else
                    e.printStackTrace();
            }
        });
        ffRatioCumm.remove(0);

        // Create plot dataset
        double[] values = new double[ffRatioCumm.size()];
        double[] indices = new double[ffRatioCumm.size()];
        for (int i=0; i< ffRatioCumm.size(); i++) {
            values[i] = ffRatioCumm.get(i);
            indices[i] = i+1;
        }
        DefaultXYDataset dataset = new DefaultXYDataset();
        dataset.addSeries("ffRatio", new double[][] {indices, values});

        // Create chart
        JFreeChart cdf = ChartFactory.createXYLineChart("Followers-Friends Ratio CDF", "CDF", "", dataset);

        // Save chart as png files
        try {
            ChartUtils.saveChartAsPNG(Paths.get(chartsDirectory, cdf.getTitle().getText().replace(" ", "_") + ".png").toFile(),
                    cdf, CHART_WIDTH, CHART_HEIGHT);
            System.out.println("\nPie Chart saved in " + chartsDirectory);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
