package no.ion.jhms.modularizer;

import java.lang.module.ModuleDescriptor;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

class OptionsParser {
    private Main.Options.Action action = Main.Options.Action.NONE;
    private final ModuleUpdater.Options updateOptions = new ModuleUpdater.Options();
    private final ModuleDescription.Options describeOptions = new ModuleDescription.Options();

    private final String[] args;
    private int argi = 0;

    static Main.Options parse(String... args) {
        return new OptionsParser(args).parse();
    }

    private OptionsParser(String... args) {
        this.args = args;
    }

    private Main.Options parse() {
        if (!hasMoreArguments()) {
            throw new UserErrorException("no arguments specified");
        }

        while (hasMoreArguments()) {
            switch (argument()) {
                case "-E":
                case "--add-exports":
                    updateOptions.addExports(parseAddExportsSpec(consumeOptionArgument()));
                    continue;
                case "-A":
                case "--add-reads":
                case "--add-requires":
                    updateOptions.addRequires(parseRequiresSpec(consumeOptionArgument()));
                    continue;
                case "-d":
                case "--describe-module":
                    setAction(Main.Options.Action.DESCRIBE);
                    consumeArgument();
                    continue;
                case "-f":
                case "--file":
                    Path jarPath = Path.of(consumeOptionArgument());
                    updateOptions.setJarPath(jarPath);
                    describeOptions.setJarPath(jarPath);
                    continue;
                case "-h":
                case "--help":
                    setAction(Main.Options.Action.HELP);
                    consumeArgument();
                    continue;
                case "-e":
                case "--main-class":
                    String mainClass = consumeOptionArgument();
                    if (mainClass.isBlank()) {
                        updateOptions.removeMainClass();
                    } else {
                        updateOptions.setMainClass(mainClass);
                    }
                    continue;
                case "-m":
                case "--module":
                    parseModuleNameAndVersion(consumeOptionArgument());
                    continue;
                case "-V":
                case "--module-version":
                    String rawModuleVersion = consumeOptionArgument();
                    parseRawVersion(rawModuleVersion);
                    continue;
                case "--remove-exports":
                    updateOptions.removeExports(consumeOptionArgument());
                    continue;
                case "--remove-requires":
                    updateOptions.removeRequires(consumeOptionArgument());
                    continue;
                case "-u":
                case "--update":
                    setAction(Main.Options.Action.UPDATE);
                    consumeArgument();
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

        return new Main.Options(action, updateOptions, describeOptions);
    }

    private void parseRawVersion(String rawModuleVersion) {
        if (rawModuleVersion.isBlank()) {
            updateOptions.removeVersion();
        } else {
            ModuleDescriptor.Version moduleVersion;
            try {
                moduleVersion = ModuleDescriptor.Version.parse(rawModuleVersion);
            } catch (IllegalArgumentException e) {
                throw new ErrorException("not a module version: " + rawModuleVersion);
            }
            updateOptions.setVersion(moduleVersion);
        }
    }

    private static ModuleUpdater.Options.Exports parseAddExportsSpec(String spec) {
        UnicodePointer pointer = new UnicodePointer(requireNonNull(spec));
        pointer.skipWhitespace();
        Optional<String> packageName = pointer.skipPackageName();
        if (packageName.isEmpty()) {
            throw new ErrorException("expected package name: " + pointer.toString());
        }
        var exports = new ModuleUpdater.Options.Exports(packageName.get());

        if (pointer.skipWhitespace() && !pointer.eof()) {
            if (!pointer.skip("to")) {
                throw new ErrorException("expected 'to' following package name: " + spec);
            } else if (!pointer.skipWhitespace()) {
                throw new ErrorException("missing module following 'to': " + spec);
            }

            do {
                Optional<String> moduleName = pointer.skipModuleName();
                if (moduleName.isEmpty()) {
                    throw new ErrorException("expected module name in to clause: " + spec);
                }
                exports.addTarget(moduleName.get());

                pointer.skipWhitespace();
                if (pointer.eof()) {
                    break;
                }

                if (!pointer.skip(",")) {
                    throw new ErrorException("expected ',' following " + moduleName.get() + ": " + spec);
                }
                pointer.skipWhitespace();
            } while (true);
        }

        return exports;
    }

    private void setAction(Main.Options.Action action) {
        if (this.action != Main.Options.Action.NONE) {
            throw new UserErrorException(action.longOption() + " conflicts with " + this.action.longOption());
        }
        this.action = action;
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

    private void parseModuleNameAndVersion(String nameAndVersion) {
        int atIndex = nameAndVersion.indexOf('@');

        final String moduleName;
        if (atIndex == -1) {
            moduleName = nameAndVersion;
        } else {
            moduleName = nameAndVersion.substring(0, atIndex);

            String rawVersion = nameAndVersion.substring(atIndex + 1);
            parseRawVersion(rawVersion);
        }

        var pointer = new UnicodePointer(moduleName);
        String normalizedModuleName = pointer.skipModuleName()
                .orElseThrow(() -> new UserErrorException("not a valid module name: " + moduleName));
        updateOptions.setModuleName(normalizedModuleName);
    }

    private static ModuleUpdater.Options.Requires parseRequiresSpec(String spec) {
        var pointer = new UnicodePointer(spec);

        var modifiers = EnumSet.noneOf(ModuleUpdater.Options.Requires.Modifier.class);
        do {
            if (pointer.skip("transitive")) {
                if (!modifiers.add(ModuleUpdater.Options.Requires.Modifier.TRANSITIVE)) {
                    throw new ErrorException("transitive specified twice: " + spec);
                }
            } else if (pointer.skip("static")) {
                if (!modifiers.add(ModuleUpdater.Options.Requires.Modifier.STATIC)) {
                    throw new ErrorException("static specified twice: " + spec);
                }
            } else {
                break;
            }

            if (!pointer.skipWhitespace()) {
                throw new ErrorException("missing module name: " + spec);
            }
        } while (true);

        Optional<String> moduleName = pointer.skipModuleName();
        if (moduleName.isEmpty()) {
            throw new ErrorException("missing module name: " + spec);
        }
        ModuleUpdater.Options.Requires requires = new ModuleUpdater.Options.Requires(moduleName.get());

        modifiers.forEach(requires::setModifier);

        if (!pointer.eof()) {
            if (!pointer.skip("@")) {
                throw new ErrorException("expected '@' following module name " + moduleName.get() + ": " + spec);
            }
            var rawVersion = pointer.toString();
            ModuleDescriptor.Version version;
            try {
                version = ModuleDescriptor.Version.parse(rawVersion);
            } catch (IllegalArgumentException e) {
                throw new ErrorException("not a valid module version: " + rawVersion + ": " + e.getMessage());
            }

            requires.setVersion(version);
        }

        return requires;
    }
}
