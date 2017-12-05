
/**
 * CLASS DESCRIPTION HERE
 * Created by alexaldev
 * Date: 28/11/2017
 */
public class Main {

    public static void main(String[] args){
//
//        TweetsCollector.newInstance("tuesdaythoughts",27017)
//                      .startCollecting();

        TweetsCollector.newInstance("tuesdaythoughts",27017)
                .printCollection();


//        if (args.length < 2){
//            System.out.println("Please call the program with the mongoDBPort and keyword to stream." +
//                    "For example: java -jar Main.jar 27017 \"KEYWORD\" ");
//        }
//
//        else {
//            TweetsCollector.newInstance().startCollecting(
//                    Integer.parseInt(args[0]),args[1]
//            );
//        }

    }
}
