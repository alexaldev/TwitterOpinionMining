package args;

import com.beust.jcommander.Parameter;

/**
 * Arguments for use without any command
 */
public class MainArgs {

    @Parameter(names = {"-h", "--help"}, help = true, description = "Prints help message")
    private boolean help;

    public boolean wantsHelp() {
        return help;
    }
}
