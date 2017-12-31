package domain;


/**
 * The main tweet model.
 * Created by alexaldev
 * @since 1.0
 */
public class TweetModel {

    public static class Builder {

        private long tweetID;

        private long userID;

        private String tweetText;

        private String transformedTweetText;

        //Not sure if needed
        private String label;

        private double positiveProbability;

        private double negativeProbability;

        private double neutralProbability;

        public Builder setTweetID(long tweetID){
            this.tweetID = tweetID;
            return this;
        }

        public Builder setUserID(long userID){
            this.userID= userID;
            return this;
        }

        public Builder setTweetText(String tweetText){
            this.tweetText = tweetText;
            return this;
        }

        public Builder setTransformedTweetText(String transformedTweetText){
            this.transformedTweetText = transformedTweetText;
            return this;
        }

        public Builder setLabel(String label){
            this.label = label;
            return this;
        }

        public Builder setPositiveProbability(double positiveProbability){
            this.positiveProbability = positiveProbability;
            return this;
        }

        public Builder setNegativeProbability(double negativeProbability){
            this.negativeProbability = negativeProbability;
            return this;
        }

        public Builder setNeutralProbability(double neutralProbability){
            this.neutralProbability = neutralProbability;
            return this;
        }

        public TweetModel create(){
            return new TweetModel(this);
        }
    }

    // End of Builder class -------------------------------------------------------------->

    private long tweetID;

    private long userID;

    private String tweetText;

    private String transformedTweetText;

    //TODO Possible setters for these fields too, will see

    private String label;

    private double positiveProbability;

    private double negativeProbability;

    private double neutralProbability;

    /**
     * Empty constructor needed for MongoDB parse
     */
    public TweetModel(){}

    private TweetModel(Builder builder){

        this.tweetID = builder.tweetID;
        this.userID = builder.userID;
        this.tweetText = builder.tweetText;
        this.transformedTweetText = builder.transformedTweetText;
        this.label = builder.label;
        this.positiveProbability = builder.positiveProbability;
        this.negativeProbability = builder.negativeProbability;
        this.neutralProbability = builder.neutralProbability;

    }

    public TweetModel copy(){

        TweetModel other = new TweetModel();
        other.tweetID = this.tweetID;
        other.userID = this.userID;
        other.tweetText = this.tweetText;
        other.transformedTweetText = this.transformedTweetText;
        other.label = this.label;
        other.positiveProbability = this.positiveProbability;
        other.negativeProbability = this.negativeProbability;
        other.neutralProbability = this.neutralProbability;
        return other;
    }

    public TweetModel copy(String tweetText) {
        TweetModel newTweet = this.copy();
        newTweet.setTweetText(tweetText);
        return newTweet;
    }

    public long getTweetID() {
        return tweetID;
    }

    public long getUserID() {
        return userID;
    }

    public String getTweetText() {
        return tweetText;
    }

    public String getTransformedTweetText() {
        return transformedTweetText;
    }

    public double getNegativeProbability() {
        return negativeProbability;
    }

    public double getNeutralProbability() {
        return neutralProbability;
    }

    public double getPositiveProbability() {
        return positiveProbability;
    }

    public String getLabel() {
        return label;
    }

    public void setTweetID(long tweetID) {
        this.tweetID = tweetID;
    }

    public void setUserID(long userID) {
        this.userID = userID;
    }

    public void setTweetText(String tweetText) {
        this.tweetText = tweetText;
    }

    public void setTransformedTweetText(String transformedTweetText) {
        this.transformedTweetText = transformedTweetText;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setPositiveProbability(double positiveProbability) {
        this.positiveProbability = positiveProbability;
    }

    public void setNegativeProbability(double negativeProbability) {
        this.negativeProbability = negativeProbability;
    }

    public void setNeutralProbability(double neutralProbability) {
        this.neutralProbability = neutralProbability;
    }

    @Override
    public String toString() {
        return "Tweet id: " +
                tweetID +
                "\n Tweet: " +
                tweetText +
                "\n" +
                "Label: " + label + ", probability: {neg: " + negativeProbability + ", neut: " + neutralProbability +
                ", pos: " + positiveProbability + "}";
    }
}
