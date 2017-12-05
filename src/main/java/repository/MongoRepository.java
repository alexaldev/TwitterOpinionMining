package repository;

import com.mongodb.Block;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import domain.TweetModel;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;

import java.util.List;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

/**
 * CLASS DESCRIPTION HERE
 * Created by alexaldev
 * Date: 28/11/2017
 */
public class MongoRepository {


    /**
     * Factory class to create instances.
     * @param host
     * @param collectionName
     * @param port
     * @return
     */
    public static MongoRepository newInstance(String host,
                                              String collectionName,
                                              int port){

        return new MongoRepository(host,collectionName,port);
    }

    // --------------- END OF FACTORY -------------------------->

    private static final String DEFAULT_DATABASE_NAME = "tweetsDb";

    private MongoClient mongoClient;
    private MongoDatabase mongoDatabase;
    private String collectionName;
    private MongoCollection<TweetModel> collection;

    private MongoRepository(String host,
                            String collectionName,
                            int port){

        System.out.println("Initiating Mongo client on port: " + port);

        //Initiate client and database reference
        this.mongoClient = new MongoClient(host,port);

        this.mongoDatabase = this.mongoClient.getDatabase(DEFAULT_DATABASE_NAME);

        this.collectionName = collectionName;

        //Configure a POJO codec registry to automatically parse Tweet model to Mongo Document model

        CodecRegistry pojoCodecRegistry = fromRegistries(MongoClient.getDefaultCodecRegistry(),
                fromProviders(PojoCodecProvider.builder().automatic(true).build()));

        this.collection = mongoDatabase.
                getCollection(collectionName,TweetModel.class)
                .withCodecRegistry(pojoCodecRegistry);

        System.out.println("Mongo Repository setup ready on collection: " + collectionName);

    }

    /**
     * Just cleanup things, no necessary still
     */
    public void disconnect(){

    }

    public void addItem(TweetModel tweet){
        this.collection.insertOne(tweet);
    }

    public void addItems(List<TweetModel> tweetModelList){

    }

    public List<TweetModel> query(MongoQuery query){
        return query.getResults();
    }

    public void printCollection(){
        Block<TweetModel> printBlock = tweetModel -> System.out.println(tweetModel.toString());

        this.collection.find().forEach(printBlock);
    }

    public void dropCollection(String collectionName){
        MongoCollection<TweetModel> collectionToDrop = this.mongoDatabase.getCollection(collectionName,TweetModel.class);
        collectionToDrop.drop();
    }

}
