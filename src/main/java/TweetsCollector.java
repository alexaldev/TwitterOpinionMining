import com.mongodb.client.MongoCursor;
import domain.TweetModel;
import domain.TweetModelParser;
import repository.MaxCountReachedException;
import repository.MongoRepository;
import twitter.Constants;
import twitter4j.*;
import twitter4j.conf.ConfigurationBuilder;

import java.util.Iterator;

public class TweetsCollector implements StatusListener {

    private static final int MAX_TWEETS_PER_COLLECTION = 1500;

    private final TwitterStream streamInstance;
    private final MongoRepository repository;
    private final String collectionName;

    /**
     * Creates a new instance of the collector. Currently you cannot
     * provide a different database name besides the default and you should deal with this fact straight.
     * @param collectionName the collection of the default database you want to reference.
     * @param mongoDBPort the port which Mongo is open right now.
     * @return
     */
    static TweetsCollector newInstance(String collectionName,
                                       String databaseName,
                                       String mongoDBHost,
                                       int mongoDBPort){

        return new TweetsCollector(collectionName,databaseName,mongoDBHost,mongoDBPort);
    }

    private TweetsCollector(String collectionName,
                            String databaseName,
                            String mongoDBHost,
                            int mongoDBPort){

        //If we use a default configuration, let's set it up here

        ConfigurationBuilder builder = new ConfigurationBuilder()
                .setDebugEnabled(true)
                .setOAuthConsumerKey(Constants.CONSUMER_KEY)
                .setOAuthConsumerSecret(Constants.CONSUMER_SECRET)
                .setOAuthAccessToken(Constants.ACCESS_TOKEN)
                .setOAuthAccessTokenSecret(Constants.ACCESS_SECRET);

        streamInstance = new TwitterStreamFactory(builder.build()).getInstance();

        //Initialize the repository on the local host.
        this.repository = MongoRepository.newInstance(collectionName,databaseName,mongoDBHost,mongoDBPort,MAX_TWEETS_PER_COLLECTION);

        this.collectionName = collectionName;
    }



    /**
     * Opens the twitter stream and filters on the given keyword.
     * While open, each time a new tweet arrives in the stream, it will be
     * saved in the database configured by the other parameters.
     */
    public void startCollecting(){

        streamInstance.addListener(this);

        System.out.println("Starting listening for tweets...");

        streamInstance.filter(collectionName);

    }

    private void closeConnectionsAndExit(){
        streamInstance.removeListener(this);
        this.repository.disconnect();
        System.exit(0);
    }

    /**
     * Prints the collection you defined when initiating the collector.
     *
     * @param s: short printing
     */
    void printCollection(boolean s){
        this.repository.printCollection(s);
    }


    /**
     * Check if the given tweet object provided by the stream should be saved in the repository.
     * Current configuration is that the tweet MUST be in the English language and not a retweet.
     * @param status the tweet object
     * @return true if in English language, otherwise false.
     */
    private boolean tweetAccepted(Status status){
        return ( status.getLang().equals("en")  &&
                !status.isRetweet());
    }

    /**
     * Callback method fired from the stream instance each time a new tweet arrives on the stream.
     * @param status
     */
    @Override
    public void onStatus(Status status) {

        //DEBUG
        //System.out.println("Tweet received");

        //First we must check that the tweet contained is in the English language
        if (tweetAccepted(status)){

            //DEBUG
            //System.out.println("Tweet accepted. Parsing and saving to repository...");

            //If so, we should parse it the to local tweet model first and then save it immediately in the repository
            try {
                this.repository.addItem(
                        TweetModelParser.parseFrom(status)
                );
            } catch (MaxCountReachedException e) {
                System.out.println("Max count of collection reached: " + MAX_TWEETS_PER_COLLECTION);
                System.out.println("Thank you for the collection, exiting...");
                this.closeConnectionsAndExit();
            }
        }
        else {
            //DEBUG
            //System.out.println("Tweet rejected");
        }



    }

    @Override
    public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {
        System.out.println("onDeletionNotice() called:" + statusDeletionNotice.getStatusId());

    }

    @Override
    public void onTrackLimitationNotice(int i) {
    }

    @Override
    public void onScrubGeo(long l, long l1) {

    }

    @Override
    public void onStallWarning(StallWarning stallWarning) {

    }

    @Override
    public void onException(Exception e) {
        e.printStackTrace();
    }




}
