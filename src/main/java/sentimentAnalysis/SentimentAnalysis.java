package sentimentAnalysis;

import repository.MongoRepository;

public abstract class SentimentAnalysis {

    /**
     * MongoReository that will be work on
     */
    protected MongoRepository repo;

    public SentimentAnalysis(MongoRepository repo) {
        this.repo = repo;
    }


}
