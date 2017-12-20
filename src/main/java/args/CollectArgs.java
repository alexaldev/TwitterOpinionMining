package args;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters(commandDescription = "Collects tweets and stores them in a mongoDB collection")
public class CollectArgs extends Args {

    @Parameter(required = true, description = "<search keyword>")
    private String hashtag;

    public String getHashtag() {
        return hashtag;
    }
}
