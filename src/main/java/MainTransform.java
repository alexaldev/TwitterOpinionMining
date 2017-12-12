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
                .setTweetText("Today 124allafi we  ... &they tried too put a bomb into Exacrhia and they were dissapointed")
                .create();

        System.out.println("Transforming tweet...");
        tweetModel = TweetTransformer.newInstance().transformTweet(tweetModel);

        System.out.println(tweetModel.getTweetText());
    }
}
