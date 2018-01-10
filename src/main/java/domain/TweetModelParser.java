package domain;

import twitter4j.Status;

import java.util.ArrayList;
import java.util.List;

/**
 * Parser class to parse {@link Status} objects received from the Twitter API
 * to {@link TweetModel} objects.
 * Created by alexal
 */
public class TweetModelParser {

    private TweetModelParser(){}

    public List<TweetModel> parseFrom(List<Status> statusList){

        List<TweetModel> result = new ArrayList<>();

        statusList.forEach(status -> result.add(parseFrom(status)));

        return result;

    }

    /**
     * Parses the status object to the main tweet model.
     * @param status
     * @return
     */
    public static TweetModel parseFrom(Status status){
        return new TweetModel.Builder()
                .setUserID(status.getUser().getId())
                .setUserFollowersCount(status.getUser().getFollowersCount())
                .setUserFriendsCount(status.getUser().getFriendsCount())
                .setTweetID(status.getId())
                .setTweetText(status.getText())
                .create();
    }

}
