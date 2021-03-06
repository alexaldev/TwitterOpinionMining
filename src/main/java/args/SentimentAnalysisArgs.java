package args;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 * 'tweet-analyze' command arguments
 */
@Parameters(commandDescription = "Makes sentiment analysis in tweets")
public class SentimentAnalysisArgs extends Args {

    @Parameter(names = {"-d", "--charts-directory"}, description = "Directory that charts will be stored.")
    private String chartsDirectory = ".";

    public String getChartsDirectory() {
        return chartsDirectory;
    }
}
