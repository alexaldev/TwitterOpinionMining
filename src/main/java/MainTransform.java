import domain.TweetModel;

/**
 * CLASS DESCRIPTION HERE
 * Created by alexaldev
 * Date: 12/12/2017
 */
public class MainTransform {

    public static void main(String[] args) {

        TweetModel tweetModel = new TweetModel.Builder()
                .setTweetID(102)
                .setTweetText("It was awesome so god damn!111!! awesome the celebrations were ... AWESOME ")
                .create();

        System.out.println("Transforming tweet...");
        tweetModel = TweetTransformer.newInstance().transformTweet(tweetModel);

        System.out.println(tweetModel.getTweetText());
    }
}
