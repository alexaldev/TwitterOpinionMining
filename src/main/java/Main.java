import java.util.Scanner;

/**
 * CLASS DESCRIPTION HERE
 * Created by alexaldev
 * Date: 28/11/2017
 */
public class Main {

    private static final String WELCOME_MESSAGE =
            "\t\t\t========MO.TE.CO========\n" +
            "\t========MongoTweets Collector========\n" +
            "Program to collect tweets(duhh..) based on the given hashtag and inserts it in a mongoDB with an open port.";

    private static final String ARGS_WRONG_MESSAGE = "FAIL\nPlease provide 2 arguments: <portNumber> <keyword/hashtag>";

    public static void main(String[] args){

        System.out.println(WELCOME_MESSAGE);

        if (args.length != 2){
            System.out.println(ARGS_WRONG_MESSAGE);
        }
        else {
            System.out.println("MongoDB Port: " + args[0]);
            System.out.println("Hashtag: " + args[1]);

            System.out.println("Will save in collection " + args[1] + " with current filter configuration.Go?(y)");

            Scanner scanner = new Scanner(System.in);
            String answer = scanner.next();
            if (answer.toLowerCase().equals("y")){
                TweetsCollector.newInstance(args[1],Integer.parseInt(args[0]))
                        .startCollecting();
            }
            else {
                System.out.println("Bye ");
            }

        }

    }
}
