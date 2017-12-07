import java.util.Scanner;

/**
 * CLASS DESCRIPTION HERE
 * Created by alexal
 * Date: 7/12/2017
 */
public class MainPrint {

    private static final String WELCOME_MESSAGE =
            "\t\t\t========MO.TE.CO. PR========\n" +
                    "\t========MongoTweets Collector Printer========\n" +
                    "Program to print the collected tweets(duhh..)";

    private static final String ARGS_WRONG_MESSAGE = "FAIL\nPlease provide 2 arguments: <portNumber> <collectionName>";


    public static void main(String[] args){

        System.out.println(WELCOME_MESSAGE);

        if (args.length != 2){
            System.out.println(ARGS_WRONG_MESSAGE);
        }
        else {
            System.out.println("MongoDB Port: " + args[0]);
            System.out.println("Collection name: " + args[1]);
            System.out.println("Will print collection " + args[1] + " Go?(y)");

            Scanner scanner = new Scanner(System.in);
            String answer = scanner.next();

            if (answer.toLowerCase().equals("y")){
                TweetsCollector.newInstance(args[1],Integer.parseInt(args[0])).printCollection();
            }
            else {
                System.out.println("Bye ");
            }

        }

    }
}
