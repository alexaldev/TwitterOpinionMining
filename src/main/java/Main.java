import args.*;
import com.beust.jcommander.JCommander;
import domain.TweetModel;
import repository.MongoRepository;

/**
 * CLASS DESCRIPTION HERE
 * Created by alexaldev
 * Date: 28/11/2017
 */
public class Main {

    private static final String PROGRAM_NAME = "Main";
    private static final String WELCOME_MESSAGE =
            "\t\t\t========MO.TE.CO========\n" +
            "\t========MongoTweets Collector========\n" +
            "Program to collect tweets(duhh..) based on the given hashtag and inserts it in a mongoDB with an open port.\n";
    private static final String DATABASE_NAME = "tweetsDb";

    public static void main(String[] args){

        /* CLI parsing */

        MainArgs mainArgs = new MainArgs();

        CollectArgs collectArgs = new CollectArgs();
        PrintCollectionArgs printArgs = new PrintCollectionArgs();
        TransformArgs transformArgs = new TransformArgs();

        JCommander jc = JCommander.newBuilder()
                .addObject(mainArgs)
                .addCommand("collect", collectArgs)
                .addCommand("print-collection", printArgs)
                .addCommand("transform", transformArgs)
                .build();

        jc.setProgramName(PROGRAM_NAME);
        jc.parse(args);

        // Print help messages
        if (args.length < 1 || mainArgs.wantsHelp()) {
            System.out.println(WELCOME_MESSAGE);
            jc.usage();
            System.exit(1);
        } else {
            Args command = (Args) jc.getCommands().get(jc.getParsedCommand()).getObjects().get(0);

            if (command.wantsHelp()) {
                jc.usage(jc.getParsedCommand());
                System.exit(1);
            }
        }

        /* <--- End of CLI parsing */

        switch (jc.getParsedCommand()) {
            case "collect":

                TweetsCollector.newInstance(collectArgs.getHashtag(), DATABASE_NAME, collectArgs.getMongoHost(), collectArgs.getMongoPort())
                        .startCollecting();

                break;
            case "print-collection":

                TweetsCollector.newInstance(printArgs.getHashtag(),DATABASE_NAME, printArgs.getMongoHost(), printArgs.getMongoPort())
                        .printCollection(printArgs.isShort());

                break;
            case "transform":
/*
                TweetModel tweetModel = new TweetModel.Builder()
                        .setTweetID(102)
                        .setTweetText("It was awesome so god damn!111!! awesome the celebrations were ... AWESOME ")
                        .create();

                System.out.println("Transforming tweet...");
                tweetModel = TweetTransformer.newInstance().transformTweet(tweetModel);

                System.out.println(tweetModel.getTweetText());
*/
                SentimentAnalysis sa = new SentimentAnalysis(MongoRepository.newInstance(transformArgs.getHashtag()));
                sa.analyze();
                sa.printFrequents(50);
                sa.printSentiment();
                break;
        }

    }
}
