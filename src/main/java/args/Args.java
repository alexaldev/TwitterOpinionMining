package args;

import com.beust.jcommander.Parameter;

/**
 * Base Class for commands arguments Classes
 */
public class Args {

    @Parameter(names = {"-h", "--help"}, help = true, description = "Prints help message")
    private boolean help;

    @Parameter(names = {"-H", "--mongoHost"}, description = "MongoDB Host")
    private String mongoHost = "localhost";

    @Parameter(names = {"-p", "--mongoPort"}, description = "MongoDB Port")
    private int mongoPort = 27017;

    @Parameter(required = true, description = "<search keyword>")
    private String hashtag;

    public String getHashtag() {
        return hashtag;
    }

    public boolean wantsHelp() {
        return help;
    }

    public String getMongoHost() {
        return mongoHost;
    }

    public int getMongoPort() {
        return mongoPort;
    }
}
