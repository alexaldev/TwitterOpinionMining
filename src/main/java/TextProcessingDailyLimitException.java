public class TextProcessingDailyLimitException extends Exception {

    public TextProcessingDailyLimitException() {
        super("503 Bad request response received from text-processing.com. Daily request limit has been reached.");
    }
}
