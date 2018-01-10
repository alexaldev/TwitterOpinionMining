import args.*;
import com.beust.jcommander.JCommander;
import repository.MongoRepository;
import sentimentAnalysis.TweetSentimentAnalysis;
import sentimentAnalysis.UserSentimentAnalysis;

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
        SentimentAnalysisArgs sentimentAnalysisArgs = new SentimentAnalysisArgs();

        JCommander jc = JCommander.newBuilder()
                .addObject(mainArgs)
                .addCommand("collect", collectArgs)
                .addCommand("print-collection", printArgs)
                .addCommand("tweet-analyze", sentimentAnalysisArgs)
                .addCommand("user-analyze", sentimentAnalysisArgs)
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

        // Command based execution
        switch (jc.getParsedCommand()) {
            case "collect":
                TweetsCollector.newInstance(collectArgs.getHashtag(), DATABASE_NAME, collectArgs.getMongoHost(), collectArgs.getMongoPort())
                        .startCollecting();
                break;
            case "print-collection":
                TweetsCollector.newInstance(printArgs.getHashtag(),DATABASE_NAME, printArgs.getMongoHost(), printArgs.getMongoPort())
                        .printCollection(printArgs.isShort());
                break;
            case "tweet-analyze":
                TweetSentimentAnalysis sa = new TweetSentimentAnalysis(MongoRepository.newInstance(sentimentAnalysisArgs.getHashtag()));
                sa.analyze();
                sa.printFrequents(50, sentimentAnalysisArgs.getChartsDirectory());
                sa.printSentiment(sentimentAnalysisArgs.getChartsDirectory());
                break;
            case "user-analyze":
                UserSentimentAnalysis us = new UserSentimentAnalysis(MongoRepository.newInstance(sentimentAnalysisArgs.getHashtag()));
                us.storeUsersSentimentScore();
                us.produceCumulativeDistributionFrequency(sentimentAnalysisArgs.getChartsDirectory());
                break;
        }

    }
}
