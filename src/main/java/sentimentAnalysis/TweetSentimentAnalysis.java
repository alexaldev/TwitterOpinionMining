package sentimentAnalysis;

import com.mongodb.Block;
import domain.TweetModel;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.xy.DefaultXYDataset;
import org.json.JSONObject;
import repository.MongoRepository;
import utils.TransformUtil;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class TweetSentimentAnalysis extends SentimentAnalysis {

    /**
     * URL of sentiment analysis web api
     */
    private static final String SENTIMENT_URL = "http://text-processing.com/api/sentiment/";

    /**
     * width and height of charts which are produced by this class
     */
    private static final int CHART_WIDTH = 640;
    private static final int CHART_HEIGHT = 480;

    //Taken from Apache Lucene project
    /*private static final HashSet<String> STOP_WORDS = new HashSet<>(
            Arrays.asList("a", "an", "and", "are", "as", "at", "be", "but", "by",
                    "for", "if", "in", "into", "is", "it",
                    "no", "not", "of", "on", "or", "such",
                    "that", "the", "their", "then", "there", "these",
                    "they", "this", "to", "was", "will", "with"));*/

    /**
     * Stop words list, taken from NLTK library
     */
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

    /**
     * Map that counts words appearances in repo's tweets
     */
    private Map<String, Integer> frequents;

    /**
     * Map that counts sentiment probabilities in repo's tweets
     */
    private Map<String, Double> sentimentProbabilities;


    public TweetSentimentAnalysis(MongoRepository repo) {
        super(repo);
        this.frequents = new HashMap<>();
        this.sentimentProbabilities = new HashMap<>();
    }

    /**
     * Returns true if s belongs to stop words list
     */
    private static boolean isStopWord(String s) {
        return STOP_WORDS.contains(s);
    }

    /**
     * Applies bellow transformations in tweetText
     * - Clear links
     * - Clear collection keyword
     * - Clear non-alphabetic characters
     * - Convert all letters to lower case
     */
    private String transformTweet(String tweetText) {
        //Remove any links
        tweetText = TransformUtil.clearLinks(tweetText);

        //Remove collection name (hashtag) from tweet
        tweetText = TransformUtil.removeCollectionKeyword(tweetText, repo.getCollectionName());

        //First remove all the non-alphabetic symbols. Better as a first step
        tweetText = TransformUtil.onlyAlphabetic(tweetText);

        //Normalize the text (toLowerCase)
        tweetText = TransformUtil.normalize(tweetText);

        return tweetText;
    }

    /**
     * Removes all stop words from tweetText
     */
    private String removeStopWords(String tweetText) {

        // Tokenize tweet text
        List<String> tokenizedTweet = TransformUtil.tokenizeToList(tweetText);

        // Remove stopwords
        List<String> tweetWithoutStopWords = new ArrayList<>();
        for (String word: tokenizedTweet)
            if ( !isStopWord(word) )
                tweetWithoutStopWords.add(word);

        //Create the updated tweet
        StringBuilder temp = new StringBuilder();
        for (String word : tweetWithoutStopWords)
            temp.append(word).append(" ");

        return temp.toString();
    }

    /**
     * Queries chosen web api for tweet's text sentiment analysis and updates tweet's sentiment label and probabilities
     *
     * @throws TextProcessingDailyLimitException when web api return http code 503, which means that daily limit has been reached
     */
    private void sentimentAnalyze(TweetModel tweet) throws IOException, TextProcessingDailyLimitException {

        // Check conditions
        if (tweet.getTransformedTweetText().equals("")) {
            System.err.println("Tweet \"" + tweet.getTweetText() + "\" does not have any text left after transformation.");
            return;
        }

        // should I stay or should I go?
        if (tweet.getLabel() != null)
            return;

        // Setting basic post request
        HttpURLConnection con = (HttpURLConnection) new URL(SENTIMENT_URL).openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");

        // Send post request
        con.setDoOutput(true);
        DataOutputStream wr = new DataOutputStream(con.getOutputStream());
        wr.writeBytes("text=" + tweet.getTransformedTweetText());
        wr.flush();
        wr.close();

        // Check response code
        int responseCode = con.getResponseCode();
        if (responseCode == 400) {
            System.err.println("400 Bad request response received from text-processing.com for tweet:\n " +
                    tweet.getTweetText() +  "\n with transformed text:\n" +
                    tweet.getTransformedTweetText() + "\nOne of two following conditions has been met:" +
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

        //Update the tweet model
        tweet.setLabel(json.getString("label"));
        tweet.setNegativeProbability(probability.getDouble("neg"));
        tweet.setNeutralProbability(probability.getDouble("neutral"));
        tweet.setPositiveProbability(probability.getDouble("pos"));
    }

    /**
     * Adds tweet's sentiment probabilities to {@link #sentimentProbabilities}
     */
    private void collectSentimentProbabilities(TweetModel tweet) {
        //Update probabilities map
        sentimentProbabilities.merge("negative", tweet.getNegativeProbability(), Double::sum);
        sentimentProbabilities.merge("neutral", tweet.getNeutralProbability(), Double::sum);
        sentimentProbabilities.merge("positive", tweet.getPositiveProbability(), Double::sum);
    }

    /**
     * Calls the {@link #transformTweet(String)} method and then adds words appearances
     * in transformed tweet text to {@link #frequents}
     */
    private String transformTweetAndCollectFrequents(String tweetText) {

        String transformedTweet = transformTweet(tweetText);

        // Count words in tweet
        for (String word : TransformUtil.tokenizeToList(transformedTweet))
            frequents.merge(word, 1, Integer::sum);

        return transformedTweet;
    }

    /**
     * Does a generic analysis in repo. It transforms tweet text, collects words appearances, removes stop words,
     * does sentiment analysis and collects sentiment probabilities in each tweet in repo.
     */
    public void analyze() {

        final long totalCount = repo.getCollectionCount();
        final AtomicLong currCount = new AtomicLong(0L);

        Block<TweetModel> analysisBlock = (TweetModel tweetModel) -> {

            if (currCount.incrementAndGet() % 50 == 0) {
                System.out.printf("\rAnalysing...%d%%", currCount.get()*100/totalCount);
            }

            // Transform tweet and collect word appearances
            String transformedTweetText = transformTweetAndCollectFrequents(tweetModel.getTweetText());

            // Remove stop words from tweet
            transformedTweetText = removeStopWords(transformedTweetText);

            tweetModel.setTransformedTweetText(transformedTweetText);

            // Query text-processing.com for sentiment analysis
            try {
                // Sentiment Analysis on tweet and store probabilities in this tweet model
                sentimentAnalyze(tweetModel);

                // Take sentiment probabilities from this tweet model and add them to sum
                collectSentimentProbabilities(tweetModel);

                // Update tweet model in collection
                repo.updateItem(tweetModel);

            } catch (IOException e) {
                e.printStackTrace();
            } catch (TextProcessingDailyLimitException e) {
                e.printStackTrace();
            }
        };

        repo.getCollectionIterable().forEach(analysisBlock);

    }

    /**
     * Prints to screen top N frequent words in repo with and without stop words and produces following charts:
     * - All words count line chart
     * - Top N frequent words included stop words bar chart
     * - Top N frequent words without stop words bar chart
     */
    public void printFrequents(int n, String chartsDirectory) {

        // Check if analysis has been made or not
        if (frequents.isEmpty()) {
            System.err.println("A call to analyze has not been made. Word count for repo will start now...");
            repo.getCollectionIterable().forEach( (Block<TweetModel>) (TweetModel tweet) ->
                    transformTweetAndCollectFrequents(tweet.getTweetText()));
            System.err.println("Word counting finished.");
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

        // JFreeCharts datasets for e charts: all words line chart, top n words included stop words bar chart,
        // top n words without stop words bar chart
        DefaultCategoryDataset allWordsDataset = new DefaultCategoryDataset();
        sortedFrequents.forEach( (String key, Integer val) -> allWordsDataset.addValue(val, "words", key));

        DefaultXYDataset allWordsDatasetXY = new DefaultXYDataset();

        ArrayList<Double> valuesList = new ArrayList<>();
        sortedFrequents.values().forEach(i -> valuesList.add(Math.log10(i)));
        double[] values = new double[valuesList.size()];
        double[] indices = new double[valuesList.size()];
        for (int i=0; i< valuesList.size(); i++) {
            values[i] = valuesList.get(i);
            indices[i] = Math.log10(i);
        }
        indices[0] = 0;

        allWordsDatasetXY.addSeries("words", new double[][] {indices, values});


        DefaultCategoryDataset topNWithStopwordsDataset = new DefaultCategoryDataset();
        DefaultCategoryDataset topNWithoutStopwordsDataset = new DefaultCategoryDataset();

        // Fill in topN words datasets and print to screen word counts
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

        // Create the charts
        JFreeChart lineChartAllWords = ChartFactory.createLineChart("All words count",
                "", "count", allWordsDataset,
                PlotOrientation.VERTICAL, false, false, false );
        JFreeChart plotChartAllWords = ChartFactory.createXYLineChart("All words Zipf diagram",
                "word rank (log10)", "word frequencylog(10)", allWordsDatasetXY);
        JFreeChart barChartWithStopwords = ChartFactory.createBarChart("Top " + n + " words included stopwords",
                "", "count", topNWithStopwordsDataset);
        JFreeChart barChartWithoutStopwords = ChartFactory.createBarChart("Top " + n + " words without stopwords",
                "", "count", topNWithoutStopwordsDataset);

        // Save charts as png files
        try {
            ChartUtils.saveChartAsPNG(Paths.get(chartsDirectory, lineChartAllWords.getTitle().getText().replace(" ", "_") + ".png").toFile(),
                    lineChartAllWords, CHART_WIDTH, CHART_HEIGHT);

            ChartUtils.saveChartAsPNG(Paths.get(chartsDirectory, plotChartAllWords.getTitle().getText().replace(" ", "_") + ".png").toFile(),
                    plotChartAllWords, CHART_WIDTH, CHART_HEIGHT);

            ChartUtils.saveChartAsPNG(Paths.get(chartsDirectory, barChartWithStopwords.getTitle().getText().replace(" ", "_") + ".png").toFile(),
                    barChartWithStopwords, CHART_WIDTH, CHART_HEIGHT);
            ChartUtils.saveChartAsPNG(Paths.get(chartsDirectory, barChartWithoutStopwords.getTitle().getText().replace(" ", "_") + ".png").toFile(),
                    barChartWithoutStopwords, CHART_WIDTH, CHART_HEIGHT);

            System.out.println("\nCharts saved.");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Produces a pie chart for sentiment probabilities in repo's tweets
     */
    public void printSentiment(String chartsDirectory) {

        // Check if analysis has been made or not
        if (sentimentProbabilities.isEmpty()) {
            System.err.println("A call to analyze has not been made. Start collecting now...");
            repo.getCollectionIterable().forEach( (Block<TweetModel>) this::collectSentimentProbabilities);
            System.err.println("Sentiment probabilities collection finished.");
        }

        // Create and fill chart dataset
        DefaultPieDataset dataset = new DefaultPieDataset();
        sentimentProbabilities.forEach(dataset::setValue);

        // Create chart
        JFreeChart pieChart = ChartFactory.createPieChart("Sentiment Pie Chart", dataset);

        // Save chart as png files
        try {
            ChartUtils.saveChartAsPNG(Paths.get(chartsDirectory, pieChart.getTitle().getText().replace(" ", "_") + ".png").toFile(),
                    pieChart, CHART_WIDTH, CHART_HEIGHT);
            System.out.println("\nPie Chart saved in local directory.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
