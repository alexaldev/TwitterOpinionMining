package repository;

import com.mongodb.Block;
import com.mongodb.MongoClient;
import com.mongodb.client.*;
import domain.TweetModel;
import org.bson.BsonInt64;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.conversions.Bson;

import java.util.List;

import static com.mongodb.client.model.Filters.eq;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

/**
 * Wraps connection to a mongo collection
 * Created by alexaldev
 * Date: 28/11/2017
 */
public class MongoRepository {

    private static final String DEFAULT_DATABASE_NAME = "tweetsDb";
    private static final String DEFAULT_MONGO_HOST = "localhost";
    private static final int DEFAULT_MONGO_PORT = 27017;
    private static final int DEFAULT_MAX_TWEETS_PER_COLLECTION = 1500;

    private MongoClient mongoClient;
    private MongoDatabase mongoDatabase;
    private String collectionName;
    private MongoCollection<TweetModel> collection;
    private long maxCollectionCount;
    private long currentEntriesInCollection;

    /**
     * Factory method to create instances.
     */
    public static MongoRepository newInstance(String collectionName) {
        return MongoRepository.newInstance(collectionName, DEFAULT_DATABASE_NAME);
    }
    public static MongoRepository newInstance(String collectionName, String database) {
        return MongoRepository.newInstance(collectionName, database, DEFAULT_MONGO_HOST);
    }
    public static MongoRepository newInstance(String collectionName, String database, String host) {
        return MongoRepository.newInstance(collectionName, database, host, DEFAULT_MONGO_PORT);
    }
    public static MongoRepository newInstance(String collectionName, String database, String host, int port) {
        return MongoRepository.newInstance(collectionName, database, host, port, DEFAULT_MAX_TWEETS_PER_COLLECTION);
    }
    public static MongoRepository newInstance(String collectionName,
                                              String database,
                                              String host,
                                              int port,
                                              int maxCollectionCount){

        return new MongoRepository(collectionName,database,host,port,maxCollectionCount);
    }


    private MongoRepository(String collectionName,
                            String database,
                            String host,
                            int port,
                            long maxCollectionCount) {

        //DEBUG
        //System.out.println("Initiating Mongo client on port: " + port);

        //Initiate client and database reference
        this.mongoClient = new MongoClient(host,port);
        this.mongoDatabase = this.mongoClient.getDatabase(database);
        this.collectionName = collectionName;

        //Configure a POJO codec registry to automatically parse Tweet model to Mongo Document model
        CodecRegistry pojoCodecRegistry = fromRegistries(MongoClient.getDefaultCodecRegistry(),
                fromProviders(PojoCodecProvider.builder().automatic(true).build()));

        // Get or create collection in Database
        this.collection = mongoDatabase.
                getCollection(collectionName,TweetModel.class)
                .withCodecRegistry(pojoCodecRegistry);

        this.currentEntriesInCollection = this.collection.count();
        this.maxCollectionCount = maxCollectionCount;

        System.out.println("Mongo Repository setup ready on collection: " + collectionName + "\nCurrent entries count: " + this.currentEntriesInCollection);
    }

    public String getCollectionName() {
        return collectionName;
    }

    public String getDatabaseName() {
        return mongoDatabase.getName();
    }

    public String getHostname() {
        return mongoClient.getConnectPoint().split(":")[0];
    }

    public int getPort() {
        return Integer.parseInt(mongoClient.getConnectPoint().split(":")[1]);
    }

    public FindIterable<TweetModel> getCollectionIterable(){
        return this.collection.find();
    }

    public AggregateIterable<TweetModel> collectionAggregate(List<Bson> aggregates) {
        return collection.aggregate(aggregates);
    }

    public <T> DistinctIterable<T> distinctCollection(String fieldname, Class<T> type) {
        return collection.distinct(fieldname, type);
    }

    public long getCollectionCount() {
        return collection.count();
    }

    /**
     * Just cleanup things, no necessary still
     */
    public void disconnect(){
        this.mongoDatabase = null;
        this.mongoClient = null;
        this.collection = null;
    }

    /**
     * Adds a tweet to mongo collection
     * @throws MaxCountReachedException when max number of items for this collection has been reached
     */
    public void addItem(TweetModel tweet) throws MaxCountReachedException{

        if (this.currentEntriesInCollection < maxCollectionCount){
            //DEBUG
            //System.out.println("Adding tweet #" + ++currentEntriesInCollection);
            this.collection.insertOne(tweet);
        }
        else {
            throw new MaxCountReachedException(collectionName);
        }

    }

    /**
     * Updates a tweet based on tweetID
     */
    public void updateItem(TweetModel tweet) {
        collection.replaceOne(eq("tweetID", tweet.getTweetID()), tweet);
    }

    public List<TweetModel> query(MongoQuery query){
        return query.getResults();
    }

    /**
     * Prints items of collection
     * @param s: Short printing: Will be print: 5 items, total count of items
     */
    public void printCollection(boolean s){
        int MAX_ITEMS_PRINT = 5;
        long limit = collection.count();

        if (s)
            limit = (MAX_ITEMS_PRINT > limit) ? limit : MAX_ITEMS_PRINT;

        Block<TweetModel> printBlock = tweetModel -> System.out.println(tweetModel.toString());

        this.collection.find().limit((int) limit).forEach(printBlock);

        System.out.println("Printed " + limit + " out of " + collection.count() + " items.");
    }

    public void dropCollection(String collectionName){
        MongoCollection<TweetModel> collectionToDrop = this.mongoDatabase.getCollection(collectionName,TweetModel.class);
        collectionToDrop.drop();
    }

}
