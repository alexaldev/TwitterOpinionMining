import domain.TweetModelParser;
import repository.MongoRepository;
import twitter.Constants;
import twitter4j.*;
import twitter4j.conf.ConfigurationBuilder;

public class TweetsCollector implements StatusListener{

    private TwitterStream streamInstance;
    private MongoRepository repository;
    private String filterKeyword;

    static TweetsCollector newInstance(String keyword,
                                       int mongoDBPort){
        return new TweetsCollector(keyword,mongoDBPort);
    }

    private TweetsCollector(String keyword,
                            int mongoDBPort){

        //If we use a default configuration, let's set it up here

        ConfigurationBuilder builder = new ConfigurationBuilder()
                .setDebugEnabled(true)
                .setOAuthConsumerKey(Constants.CONSUMER_KEY)
                .setOAuthConsumerSecret(Constants.CONSUMER_SECRET)
                .setOAuthAccessToken(Constants.ACCESS_TOKEN)
                .setOAuthAccessTokenSecret(Constants.ACCESS_SECRET);

        streamInstance = new TwitterStreamFactory(builder.build()).getInstance();

        this.filterKeyword = keyword;

        this.repository = MongoRepository.newInstance("localhost",keyword,mongoDBPort);
    }


    /**
     * Opens the twitter stream and filters on the given keyword.
     * While open, each time a new tweet arrives in the stream, it will be
     * saved in the database configured by the other parameters.
     */
    public void startCollecting(){

        System.out.println("Initiating repository connection");

        streamInstance.addListener(this);

        System.out.println("Starting listening for tweets...");

        streamInstance.filter(filterKeyword);


    }

    void printCollection(){
        this.repository.printCollection();
    }

    void dropCollection(String collectionName){
        this.repository.dropCollection(collectionName);
    }

    /**
     * Check if the given tweet object provided by the stream should be saved in the repository.
     * Current configuration is that the tweet MUST be in the English language.
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

        System.out.println("Tweet received");

        //First we must check that the tweet contained is in the English language
        if (tweetAccepted(status)){

            System.out.println("Tweet accepted. Parsing and saving to repository...");

            //If so, we should parse it the to local tweet model first and then save it immediately in the repository
            this.repository.addItem(
                    TweetModelParser.parseFrom(status)
            );
        }
        else {
            System.out.println("Tweet rejected");
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
