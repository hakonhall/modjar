package no.ion.jhms.modularizer;

import java.io.PrintStream;

public class Main {
    private final PrintStream out;

    public static String usageText() {
        return "Usage: modjar [OPTION...]\n" +
                "View and modify the module descriptor of a JAR\n" +
                "\n" +
                "Options:\n" +
                "  -E,--add-exports 'PACKAGE [to MODULE [, MODULE]...]'\n" +
                "                                Add an 'exports' directive.\n" +
                "  -A,--add-requires '[static|transitive] MODULE[@VERSION]'\n" +
                "                                Add a 'requires' directive.\n" +
                "  -d,--describe-module          Print module descriptor info.\n" +
                "  -f,--file FILE                Either a JAR or module-info.class.\n" +
                "  -e,--main-class CLASS         Set the main class, or remove if empty.\n" +
                "  -m,--module MODULE[@VERSION]  Set module name and optionally version.\n" +
                "  -V,--module-version VERSION   Set module version, or remove if empty.\n" +
                "     --remove-exports PACKAGE   Remove an 'exports' directive.\n" +
                "     --remove-requires MODULE   Remove a 'requires' directive.\n" +
                "  -u,--update                   Update FILE in-place. [required]\n" +
                "";
    }

    public static void main(String[] args){
        try {
            new Main(System.out).mainImpl(args);
        } catch (MainException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    /** Print to out instead of {@link System#out}. */
    public Main(PrintStream out) {
        this.out = out;
    }

    /**
     * This method is an API for running this class as the main class of a Java program.
     *
     * <ol>
     *     <li>Text that would have been written to {@link System#out} is instead written to the {@code out} parameter.</li>
     *     <li>Returning normally from this method is equivalent to exit code 0.</li>
     *     <li>Returning by throwing an exception would cause a message to be written to {@link System#err} and
     *     exit code 1.  The message is {@link MainException#getMessage() getMessage} if the exception is a subclass
     *     of {@link MainException}, or otherwise the stack trace as-if the exception escaped the {@code main} method.</li>
     *     <li>Is an instance method to allow for mocking.</li>
     * </ol>
     *
     * @param args the main() program arguments
     * @throws MainException if only the {@link Throwable#getMessage() getMessage} should be written to {@link System#err}
     */
    public void mainImpl(String... args) {
        Options options = OptionsParser.parse(args);
        mainImpl(options);
    }

    public static class Options {
        private final Action action;
        private final ModuleUpdater.Options updateOptions;
        private final ModuleDescription.Options describeOptions;

        enum Action {
            DESCRIBE("--describe-module"),
            HELP("--help"),
            NONE(""),
            UPDATE("--update");

            private final String longOption;

            Action(String longOption) {
                this.longOption = longOption;
            }

            String longOption() {
                return longOption;
            }
        }

        public Options(Action action, ModuleUpdater.Options updateOptions, ModuleDescription.Options describeOptions) {
            this.action = action;
            this.updateOptions = updateOptions;
            this.describeOptions = describeOptions;
        }

        Action action() { return action; }
        ModuleUpdater.Options updateOptions() { return updateOptions; }
        ModuleDescription.Options describeOptions() { return describeOptions; }
    }

    /** A type-safe variant of mainImpl() */
    public void mainImpl(Options options) {
        switch (options.action) {
            case DESCRIBE:
                ModuleDescription.describeModule(out, options.describeOptions);
                break;
            case HELP:
                out.print(usageText());
                break;
            case NONE:
                throw new ErrorException("nothing to do");
            case UPDATE:
                ModuleUpdater.update(out, options.updateOptions);
                break;
            default:
                throw new IllegalStateException("unknown action: " + options.action);
        }
    }
}
