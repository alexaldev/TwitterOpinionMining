import domain.TweetModel;
import utils.TransformUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * Class performing all the necessary steps described in requirements. (3th step)
 * Created by alexaldev
 * Date: 12/12/2017
 */
public class TweetTransformer {

    //Taken from Apache Lucene project
    private static final HashSet<String> STOP_WORDS = new HashSet<>(
            Arrays.asList("a", "an", "and", "are", "as", "at", "be", "but", "by",
                    "for", "if", "in", "into", "is", "it",
                    "no", "not", "of", "on", "or", "such",
                    "that", "the", "their", "then", "there", "these",
                    "they", "this", "to", "was", "will", "with"));

    public static TweetTransformer newInstance() {
        return new TweetTransformer();
    }

    private TweetTransformer() {

    }

    private boolean isStopWord(String s) {
        return STOP_WORDS.contains(s);
    }

    TweetModel transformTweet(TweetModel tweetModel) {

        String tweetText = tweetModel.getTweetText();

        System.out.println("Transforming text: " + tweetText);

        System.out.println("Transforming to only alphabetic: ");

        //First remove all the non-alphabetic symbols. Better as a first step
        tweetText = TransformUtil.onlyAlphabetic(tweetText);

        System.out.println(tweetText);

        System.out.println("Normalizing: ");
        //Normalize the text (toLowerCase)
        tweetText = TransformUtil.normalize(tweetText);

        System.out.println(tweetText);

        //Remove all stop words
        List<String> tokenizedTweet = TransformUtil.tokenizeToList(tweetText);
        List<String> tweetWithoutStopWords = new ArrayList<>();

        for (String word: tokenizedTweet) {
            System.out.println("Checking if " + word + " is stop word");

            if ( !isStopWord(word) ) {
                System.out.println("Not a stop word, keeping");
                tweetWithoutStopWords.add(word);
            }
        }

        //Create the updated tweet
        StringBuilder temp = new StringBuilder();

        for (String word : tweetWithoutStopWords){
            temp.append(word).append(" ");
        }

        //Update the tweet model
        tweetModel.setTweetText(temp.toString());

        return tweetModel;

    }

}


