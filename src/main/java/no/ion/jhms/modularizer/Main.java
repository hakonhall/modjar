package no.ion.jhms.modularizer;

import java.io.PrintStream;

public class Main {
    public static String helpText() {
        return "Usage: modjar [OPTION...]\n" +
                "View and modify the module descriptor of a modular JAR\n" +
                "\n" +
                "Options:\n" +
                "  -d,--describe-module          Print module descriptor info.\n" +
                "  -f,--file FILE                Either a JAR or module-info.class.\n" +
                "  -u,--update                   Update FILE in-place. [required]\n" +
                "  -V,--module-version VERSION   Set module version to VERSION. Use an empty\n" +
                "                                string to remove the version.\n" +
                "  --add-requires MOD[@VERSION]  Add/update 'requires' in module-info.class.\n" +
                "                                Can be prefixed with 'static' and/or 'transitive'.\n" +
                "  --remove-requires MOD         Remove 'requires' in module-info.class.\n" +
                "  --add-exports PKG[=M1,M2...]  Add (or update) an 'exports PKG' directive to\n" +
                "                                module-info.class, optionally only to the given\n" +
                "                                comma-separated list of modules.\n" +
                "\n" +
                "\n";
    }

    public void noExitMain(PrintStream out, String... args) {
        Options options = OptionsParser.parse(args);
        ModuleUpdater.go(out, options);
    }

    public static void main(String[] args){
        try {
            new Main().noExitMain(System.out, args);
        } catch (ErrorException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        } catch (UserErrorException e) {
            System.err.println(e.getMessage());
            System.err.println("try --help to get more information");
            System.exit(1);
        }
    }
}
