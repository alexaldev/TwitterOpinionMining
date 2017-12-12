package utils;


import java.util.Arrays;
import java.util.List;

/**
 * Helper class with string manipulation functions.
 * Created by alexaldev
 * Date: 12/12/2017
 */
public class TransformUtil {

    private TransformUtil(){}

    public static List<String> tokenizeToList(String s){
        return Arrays.asList(s.split("\\s+"));
    }

    public static String onlyAlphabetic(String s){
        return s.replaceAll("[^A-Za-z]+", " ");
    }

    public static String normalize(String tweet){
        return tweet.toLowerCase();
    }

}
