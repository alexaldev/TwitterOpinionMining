package sentimentAnalysis;

import com.mongodb.Block;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.DistinctIterable;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import domain.TweetModel;
import org.bson.BsonInt64;
import org.bson.Document;
import repository.MongoRepository;
import twitter.Constants;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;

import java.util.*;

public class UserSentimentAnalysis extends SentimentAnalysis {

    private Twitter twitter;

    public UserSentimentAnalysis(MongoRepository repo) {
        super(repo);
        twitter = TwitterFactory.getSingleton();
        twitter.setOAuthConsumer(Constants.CONSUMER_KEY, Constants.CONSUMER_SECRET);
        twitter.setOAuthAccessToken(new AccessToken(Constants.ACCESS_TOKEN, Constants.ACCESS_SECRET));
    }

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

    public double calculateFollowersFriendsRatio(long userID) throws TwitterException {
        int followers = twitter.showUser(userID).getFollowersCount();
        int friends = twitter.showUser(userID).getFriendsCount();

        return ((double) followers) / friends;
    }

    private double sumList(List<Double> l) {
        double sum = 0;
        for (double e : l)
            sum += e;
        return sum;
    }

    public void produceCumulativeDistributionFrequency() {
        DistinctIterable<BsonInt64> dit = repo.distinctCollection("userID", BsonInt64.class);

        List<Double> ffRatioCumm = new ArrayList<>();
        ffRatioCumm.add(0.0);

        

        dit.forEach((Block<? super BsonInt64>) (BsonInt64 userID) -> {
            try {
                double ratio = calculateFollowersFriendsRatio(userID.getValue());
                ffRatioCumm.add(ffRatioCumm.get(ffRatioCumm.size()-1) + ratio);
            } catch (TwitterException e) {
                if (e.getStatusCode() == 403 || e.getStatusCode() == 404)
                    System.err.println("User " + userID.longValue() + " does not exist or have been removed");
                else
                    e.printStackTrace();
            }
        });

        ffRatioCumm.remove(0);



        System.out.println();
    }

}
