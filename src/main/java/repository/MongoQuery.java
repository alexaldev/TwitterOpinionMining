package repository;

import domain.TweetModel;

import java.util.List;

/**
 * CLASS DESCRIPTION HERE
 * Created by alexaldev
 * Date: 28/11/2017
 */
public interface MongoQuery {

    public List<TweetModel> getResults();
}
