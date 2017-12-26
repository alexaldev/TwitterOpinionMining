import com.mongodb.Block;
import domain.TweetModel;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;
import org.json.JSONObject;
import repository.MaxCountReachedException;
import repository.MongoRepository;
import utils.TransformUtil;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class SentimentAnalysis {

    private static final String SENTIMENT_URL = "http://text-processing.com/api/sentiment/";
    private static final String CHARSET = StandardCharsets.UTF_8.name();
    private static final String NEW_COLLECTION_NAME_SUFFIX = "_test";
    private static final int CHART_WIDTH = 640;
    private static final int CHART_HEIGHT = 480;

    //Taken from Apache Lucene project
    /*private static final HashSet<String> STOP_WORDS = new HashSet<>(
            Arrays.asList("a", "an", "and", "are", "as", "at", "be", "but", "by",
                    "for", "if", "in", "into", "is", "it",
                    "no", "not", "of", "on", "or", "such",
                    "that", "the", "their", "then", "there", "these",
                    "they", "this", "to", "was", "will", "with"));*/

    // Taken from NLTK library
    private static final HashSet<String> STOP_WORDS = new HashSet<>(
            Arrays.asList("i","me","my","myself","we","our","ours","ourselves","you",
                    "your","yours","yourself","yourselves","he","him","his","himself",
                    "she","her","hers","herself","it","its","itself","they","them","their",
                    "theirs","themselves","what","which","who","whom","this","that","these",
                    "those","am","is","are","was","were","be","been","being","have","has",
                    "had","having","do","does","did","doing","a","an","the","and","but",
                    "if","or","because","as","until","while","of","at","by","for","with",
                    "about","against","between","into","through","during","before","after",
                    "above","below","to","from","up","down","in","out","on","off","over",
                    "under","again","further","then","once","here","there","when","where",
                    "why","how","all","any","both","each","few","more","most","other","some",
                    "such","no","nor","not","only","own","same","so","than","too","very","s",
                    "t","can","will","just","don","should","now"));

    private MongoRepository repo;
    private Map<String, Integer> frequents;
    private Map<String, Double> sentimentProbabilities;

    public SentimentAnalysis(MongoRepository repo) {
        this.repo = repo;
        this.frequents = new HashMap<>();
        this.sentimentProbabilities = new HashMap<>();


    }

    private static boolean isStopWord(String s) {
        return STOP_WORDS.contains(s);
    }

    public void analyze() {

        MongoRepository newRepo = MongoRepository.newInstance(repo.getCollectionName()+NEW_COLLECTION_NAME_SUFFIX,
                repo.getDatabaseName(), repo.getHostname(), repo.getPort());

        Block<TweetModel> analysisBlock = (TweetModel tweetModel) -> {
            String tweetText = tweetModel.getTweetText();

            //DEBUG
            //System.out.println("Original tweet:\n" + tweetText);

            //Remove any links
            tweetText = TransformUtil.clearLinks(tweetText);

            //Remove collection name (hashtag) from tweet
            tweetText = TransformUtil.removeCollectionKeyword(tweetText, repo.getCollectionName());

            //First remove all the non-alphabetic symbols. Better as a first step
            tweetText = TransformUtil.onlyAlphabetic(tweetText);

            //Normalize the text (toLowerCase)
            tweetText = TransformUtil.normalize(tweetText);

            // Tokenize tweet text
            List<String> tokenizedTweet = TransformUtil.tokenizeToList(tweetText);

            // Count words in tweet
            for (String word : tokenizedTweet)
                frequents.merge(word, 1, Integer::sum);

            // Remove stopwords
            List<String> tweetWithoutStopWords = new ArrayList<>();
            for (String word: tokenizedTweet)
                if ( !isStopWord(word) )
                    tweetWithoutStopWords.add(word);

            //Create the updated tweet
            StringBuilder temp = new StringBuilder();
            for (String word : tweetWithoutStopWords)
                temp.append(word).append(" ");
            tweetText = temp.toString();

            //DEBUG
            //System.out.println("Transformed tweet:\n" + tweetText);

            // Query text-processing.com for sentiment analysis
            try {
                HttpURLConnection con = (HttpURLConnection) new URL(SENTIMENT_URL).openConnection();

                // Setting basic post request
                con.setRequestMethod("POST");
                con.setRequestProperty("Content-Type", "application/json");

                // Send post request
                con.setDoOutput(true);
                DataOutputStream wr = new DataOutputStream(con.getOutputStream());
                wr.writeBytes("text=" + tweetText);
                wr.flush();
                wr.close();

                // Check response code
                int responseCode = con.getResponseCode();
                //DEBUG
                //System.out.println("Response Code : " + responseCode);
                if (responseCode == 400) {
                    System.err.println("400 Bad request response received from text-processing.com" +
                            ". One of two following conditions has been met:" +
                            "\n- no value for text is provided" +
                            "\n- text exceeds 80,000 characters");
                } else if (responseCode == 503) {
                    throw new TextProcessingDailyLimitException();
                }

                // Get response text-json
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String output;
                StringBuilder b = new StringBuilder();
                while ((output = in.readLine()) != null) {
                    b.append(output);
                }
                in.close();

                // parse response json
                JSONObject json = new JSONObject(b.toString());
                JSONObject probability = json.getJSONObject("probability");
                String label = json.getString("label");
                Double negProb = probability.getDouble("neg");
                Double neutProb = probability.getDouble("neutral");
                Double posProb = probability.getDouble("pos");

                //Update probabilities map
                sentimentProbabilities.merge("negative", negProb, Double::sum);
                sentimentProbabilities.merge("neutral", neutProb, Double::sum);
                sentimentProbabilities.merge("positive", posProb, Double::sum);

                //Update the tweet model
                TweetModel newTweetModel = tweetModel.copy();
                newTweetModel.setTweetText(temp.toString());
                newTweetModel.setLabel(label);
                newTweetModel.setNegativeProbability(negProb);
                newTweetModel.setNeutralProbability(neutProb);
                newTweetModel.setPositiveProbability(posProb);

                //add tweet model to collection
                //newRepo.addItem(newTweetModel);

                //DEBUG
                //System.out.println(newTweetModel);

            } catch (IOException e) {
                e.printStackTrace();
            } catch (TextProcessingDailyLimitException e) {
                e.printStackTrace();
            //} catch (MaxCountReachedException e) {
              //  e.printStackTrace();
            }

        };

        repo.getCollectionIterable().forEach(analysisBlock);

    }

    public void printFrequents(int n) {

        // Check if analysis has been made or not
        if (frequents.isEmpty()) {
            System.err.println("A call to analyze must be preceded before a call to printFrequents");
            return;
        }

        // sort frequents map
        LinkedHashMap<String, Integer> sortedFrequents = frequents
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Collections.reverseOrder()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));

        DefaultCategoryDataset allWordsDataset = new DefaultCategoryDataset();
        sortedFrequents.forEach( (String key, Integer val) -> allWordsDataset.addValue(val, "words", key));

        DefaultCategoryDataset topNWithStopwordsDataset = new DefaultCategoryDataset();
        DefaultCategoryDataset topNWithoutStopwordsDataset = new DefaultCategoryDataset();


        System.out.println("Top " + n + " words included stopwords:");
        int k = n;
        for (Map.Entry<String, Integer> e : sortedFrequents.entrySet()) {
            if (k < 1)
                break;
            System.out.printf("%2d. %-18s %4d\n", n-k+1, e.getKey(), e.getValue());
            topNWithStopwordsDataset.addValue(e.getValue(), e.getKey(), "word");
            k--;
        }

        System.out.println("\nTop " + n + " words without stopwords:");
        k = n;
        for (Map.Entry<String, Integer> e : sortedFrequents.entrySet()) {
            if (k < 1)
                break;
            if (!STOP_WORDS.contains(e.getKey())) {
                System.out.printf("%2d. %-18s %4d\n", n-k+1, e.getKey(), e.getValue());
                topNWithoutStopwordsDataset.addValue(e.getValue(), e.getKey(), "word");
                k--;
            }
        }

        JFreeChart lineChartAllWords = ChartFactory.createLineChart("All words count",
                "", "count", allWordsDataset,
                PlotOrientation.VERTICAL, false, false, false );
        JFreeChart barChartWithStopwords = ChartFactory.createBarChart("Top " + n + " words included stopwords",
                "", "count", topNWithStopwordsDataset);
        JFreeChart barChartWithoutStopwords = ChartFactory.createBarChart("Top " + n + " words without stopwords",
                "", "count", topNWithoutStopwordsDataset);

        try {
            ChartUtils.saveChartAsPNG(new File(lineChartAllWords.getTitle().getText().replace(" ", "_") + ".png"),
                    lineChartAllWords, CHART_WIDTH, CHART_HEIGHT);
            ChartUtils.saveChartAsPNG(new File(barChartWithStopwords.getTitle().getText().replace(" ", "_") + ".png"),
                    barChartWithStopwords, CHART_WIDTH, CHART_HEIGHT);
            ChartUtils.saveChartAsPNG(new File(barChartWithoutStopwords.getTitle().getText().replace(" ", "_") + ".png"),
                    barChartWithoutStopwords, CHART_WIDTH, CHART_HEIGHT);

            System.out.println("\nCharts saved in local directory.");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void printSentiment() {

        // Check if analysis has been made or not
        if (sentimentProbabilities.isEmpty()) {
            System.err.println("A call to analyze must be preceded before a call to printFrequents");
            return;
        }

        DefaultPieDataset dataset = new DefaultPieDataset();
        sentimentProbabilities.forEach(dataset::setValue);

        JFreeChart pieChart = ChartFactory.createPieChart("Sentiment Pie Chart", dataset);

        try {
            ChartUtils.saveChartAsPNG(new File(pieChart.getTitle().getText().replace(" ", "_") + ".png"),
                    pieChart, CHART_WIDTH, CHART_HEIGHT);
            System.out.println("\nPie Chart saved in local directory.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
