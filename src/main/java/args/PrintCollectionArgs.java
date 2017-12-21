package args;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 * 'print-collect' command arguments
 */
@Parameters(commandDescription = "Prints entries of a mongoDB collection")
public class PrintCollectionArgs extends CollectArgs {

    @Parameter(names = {"-s", "--short"}, description = "Short printing")
    private boolean s = false;

    public boolean isShort() { return s; }
}
