package args;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters(commandDescription = "Collects tweets and stores them in a mongoDB collection")
public class CollectArgs extends Args {

    @Parameter(names = {"-t", "--topic"}, required = true, description = "Hashtag or word to search Tweeter")
    private String hashtag;

    public String getHashtag() {
        return hashtag;
    }
}
