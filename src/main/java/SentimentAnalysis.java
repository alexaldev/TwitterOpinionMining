import com.mongodb.Block;
import domain.TweetModel;
import repository.MongoRepository;
import utils.TransformUtil;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class SentimentAnalysis {

    private static final String SENTIMENT_URL = "http://text-processing.com/api/sentiment/";
    private static final String CHARSET = StandardCharsets.UTF_8.name();

    //Taken from Apache Lucene project
    private static final HashSet<String> STOP_WORDS = new HashSet<>(
            Arrays.asList("a", "an", "and", "are", "as", "at", "be", "but", "by",
                    "for", "if", "in", "into", "is", "it",
                    "no", "not", "of", "on", "or", "such",
                    "that", "the", "their", "then", "there", "these",
                    "they", "this", "to", "was", "will", "with"));

    private static boolean isStopWord(String s) {
        return STOP_WORDS.contains(s);
    }

    public static void analyze(MongoRepository repo) {

        Map<String, Integer> frequents = new HashMap<>();

        Block<TweetModel> analysisBlock = tweetModel -> {
            String tweetText = tweetModel.getTweetText();

            //DEBUG
            System.out.println("Original tweet:\n" + tweetText);

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
            System.out.println("Transformed tweet:\n" + tweetText);


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
                System.out.println("Response Code : " + responseCode);

                // Get response
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String output;
                StringBuilder b = new StringBuilder();
                while ((output = in.readLine()) != null) {
                    b.append(output);
                }
                in.close();

                System.out.println(b.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }


            //Update the tweet model
            //tweetModel.setTweetText(temp.toString());
        };

        repo.getCollectionIterable().forEach(analysisBlock);

    }

}
