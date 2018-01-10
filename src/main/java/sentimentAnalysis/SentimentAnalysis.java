package sentimentAnalysis;

import repository.MongoRepository;

public abstract class SentimentAnalysis {

    /**
     * width and height of charts which are produced by this class
     */
    protected static final int CHART_WIDTH = 640;
    protected static final int CHART_HEIGHT = 480;

    /**
     * MongoReository that will be work on
     */
    protected MongoRepository repo;

    public SentimentAnalysis(MongoRepository repo) {
        this.repo = repo;
    }


}
