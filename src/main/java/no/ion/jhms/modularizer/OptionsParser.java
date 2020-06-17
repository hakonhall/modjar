package no.ion.jhms.modularizer;

import java.lang.module.ModuleDescriptor;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

class OptionsParser {
    private final Options options = new Options();
    private final String[] args;
    private int argi = 0;

    public static Options parse(String... args) {
        return new OptionsParser(args).parse();
    }

    private OptionsParser(String... args) {
        this.args = args;
    }

    private Options parse() {
        var options = new Options();

        while (hasMoreArguments()) {
            switch (argument()) {
                case "-d":
                case "--describe-module":
                    if (options.action() != Options.Action.NONE) {
                        throw new UserErrorException(argument() + " conflicts with " + options.action().longOption());
                    }
                    options.setAction(Options.Action.DESCRIBE);
                    consumeArgument();
                    continue;
                case "-f":
                case "--file":
                    options.setFile(consumeOptionArgument());
                    continue;
                case "-h":
                case "--help":
                    options.setHelp(true);
                    return options;
                case "-u":
                case "--update":
                    if (options.action() != Options.Action.NONE) {
                        throw new UserErrorException(argument() + " conflicts with " + options.action().longOption());
                    }
                    options.setAction(Options.Action.UPDATE);
                    consumeArgument();
                    continue;
                case "-V":
                case "--module-version":
                    options.setModuleVersion(consumeOptionArgument());
                    continue;
                case "--add-reads":
                case "--add-requires":
                    options.addRequires(parseRequiresSpec(consumeOptionArgument()));
                    continue;
                case "--remove-requires":
                    options.removeRequires(consumeOptionArgument());
                    continue;
                case "--add-exports":
                    options.addExports(consumeOptionArgument());
                    continue;
                case "--remove-exports":
                    options.removeExports(consumeOptionArgument());
                    continue;
                default:
                    if (argument().startsWith("-")) {
                        throw new UserErrorException("unknown option: " + argument());
                    } else {
                        // Fall-through to break outside of switch
                    }
            }

            break;
        }

        if (hasMoreArguments()) {
            throw new UserErrorException("extraneous argument: " + argument());
        }

        return options;
    }

    private boolean hasMoreArguments() { return argi < args.length; }
    private String argument() { return args[argi]; }

    /** argi points to option, argi+1 to its argument. Increments argi by 2. */
    private String consumeOptionArgument() {
        if (argi + 1 >= args.length) {
            throw new UserErrorException("option " + argument() + " requires an argument");
        } else {
            argi += 2;
            return args[argi - 1];
        }
    }

    /** Returns argument() and increments argi. */
    private String consumeArgument() { return args[argi++]; }

    public static Options.RequiresDirective parseRequiresSpec(String requiresSpec) {
        var pointer = new UnicodePointer(requiresSpec);

        var flags = EnumSet.noneOf(RequiresFlag.class);
        do {
            if (pointer.skip("transitive")) {
                if (!flags.add(RequiresFlag.TRANSITIVE)) {
                    throw new UserErrorException("transitive specified twice: " + requiresSpec);
                }
            } else if (pointer.skip("static")) {
                if (!flags.add(RequiresFlag.STATIC_PHASE)) {
                    throw new UserErrorException("static specified twice: " + requiresSpec);
                }
            } else {
                break;
            }

            if (!pointer.skipWhitespace()) {
                throw new UserErrorException("Missing module name: " + requiresSpec);
            }
        } while (true);

        Optional<String> moduleName = pointer.skipModuleName();
        if (moduleName.isEmpty()) {
            throw new UserErrorException("no module name specified: " + requiresSpec);
        }

        Optional<ModuleDescriptor.Version> version = Optional.empty();
        if (!pointer.eof()) {
            if (!pointer.skip("@")) {
                throw new UserErrorException("expected '@' after module name: " + requiresSpec);
            }
            var rawVersion = pointer.toString();
            version = Optional.of(ModuleDescriptor.Version.parse(rawVersion));
        }

        return new Options.RequiresDirective(moduleName.get(), flags, version);
    }
}
