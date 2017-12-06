package repository;

/**
 * CLASS DESCRIPTION HERE
 * Created by alexal
 * Date: 5/12/2017
 */
public class MaxCountReachedException extends Exception {

    public MaxCountReachedException(String collection){
        super("Max permitted count for collection " + collection +" reached");
    }

}
