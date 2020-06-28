package no.ion.jhms.modularizer;

import java.io.PrintStream;
import java.lang.module.ModuleDescriptor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;
import static no.ion.jhms.modularizer.Exceptions.uncheckIO;

public class ModuleUpdater {
    private final PrintStream out;
    private final Options options;

    public static class Options {
        private Path jarPath = null;
        private String moduleName = null;
        private Optional<String> mainClass = null; // null: no change, empty: remove
        private Optional<ModuleDescriptor.Version> version = null;  // null: no change, empty: remove
        private List<Options.Requires> requiresToAdd = new ArrayList<>();
        private List<String> requiresToRemove = new ArrayList<>();
        private List<Options.Exports> exportsToAdd = new ArrayList<>();
        private List<String> exportsToRemove = new ArrayList<>();

        public Options() {}

        public Options setJarPath(Path jarPath) {
            this.jarPath = requireNonNull(jarPath);
            return this;
        }

        public Options setMainClass(String mainClass) {
            this.mainClass = Optional.of(mainClass);
            return this;
        }

        public Options removeMainClass() {
            this.mainClass = Optional.empty();
            return this;
        }

        public Options setVersion(ModuleDescriptor.Version version) {
            this.version = Optional.of(version);
            return this;
        }

        public Options removeVersion() {
            this.version = Optional.empty();
            return this;
        }

        public Options setModuleName(String moduleName) {
            requireNonNull(moduleName);
            this.moduleName = new UnicodePointer(moduleName)
                    .skipModuleName()
                    .orElseThrow(() -> new ErrorException("not a module name: " + moduleName));

            return this;
        }

        public Path jarPath() { return jarPath; }
        public String moduleName() { return moduleName; }
        public Optional<String> mainClass() { return mainClass; }
        public Optional<ModuleDescriptor.Version> version() { return version; }
        public List<Requires> requiresToAdd() { return requiresToAdd; }
        public List<String> requiresToRemove() { return requiresToRemove; }
        public List<Exports> exportsToAdd() { return exportsToAdd; }
        public List<String> exportsToRemove() { return exportsToRemove; }

        public static class Requires {
            private final String moduleName;
            private Options.Requires.Modifier modifier = Options.Requires.Modifier.NONE;
            private Optional<ModuleDescriptor.Version> version = Optional.empty();

            public Requires(String moduleName) {
                this.moduleName = requireNonNull(moduleName);
            }

            public enum Modifier { NONE, STATIC, TRANSITIVE }

            public Options.Requires setModifier(Options.Requires.Modifier modifier) {
                this.modifier = requireNonNull(modifier);
                return this;
            }

            public Options.Requires setVersion(ModuleDescriptor.Version version) {
                this.version = Optional.of(version);
                return this;
            }

            public String moduleName() { return moduleName; }
            public Modifier modifier() { return modifier; }
            public Optional<ModuleDescriptor.Version> version() { return version; }
        }

        public Options addRequires(Options.Requires requires) {
            this.requiresToAdd.add(requireNonNull(requires));
            return this;
        }

        public Options removeRequires(String moduleName) {
            this.requiresToRemove.add(requireNonNull(moduleName));
            return this;
        }

        public static class Exports {
            private String packageName;
            private final List<String> moduleTargets = new ArrayList<>();

            public Exports(String packageName) {
                this.packageName = packageName;
            }

            public Options.Exports addTarget(String moduleName) {
                moduleTargets.add(moduleName);
                return this;
            }

            public String packageName() { return packageName; }
            public List<String> moduleTargets() { return moduleTargets; }
        }

        public Options addExports(Options.Exports exports) {
            this.exportsToAdd.add(exports);
            return this;
        }

        public Options removeExports(String packageName) {
            exportsToRemove.add(packageName);
            return this;
        }
    }

    public static void update(PrintStream out, Options options) { new ModuleUpdater(out, options).update(); }

    private ModuleUpdater(PrintStream out, Options options) {
        this.out = out;
        this.options = options;
    }

    public void update() {
        Path jarPath = options.jarPath;
        if (!jarPath.toString().endsWith(".jar")) {
            throw new ErrorException("JAR file must have .jar extension: " + jarPath);
        }

        boolean modified = false;
        boolean createdMinimalModuleClassGivenModuleName = false;

        Jar jar = new Jar(jarPath, out);
        Optional<ModuleInfoClass> moduleInfoClass = jar.readModuleInfoClass();
        if (moduleInfoClass.isEmpty()) {
            if (options.moduleName != null) {
                // We'll create a minimal module-info.class
                moduleInfoClass = Optional.of(MinimalModuleInfoClass.create(options.moduleName));
                modified = true;
                createdMinimalModuleClassGivenModuleName = true;
            } else {
                throw new ErrorException("no module-info.class in " + jarPath);
            }
        }

        if (!createdMinimalModuleClassGivenModuleName && options.moduleName != null &&
                !options.moduleName.equals(moduleInfoClass.get().getModuleName())) {
            moduleInfoClass.get().setModuleName(options.moduleName);
        }

        Optional<ModuleDescriptor.Version> versionOrEmptyOrNull = options.version;
        if (versionOrEmptyOrNull != null) {
            if (versionOrEmptyOrNull.isPresent()) {
                moduleInfoClass.get().setModuleVersion(versionOrEmptyOrNull.get());
                modified = true;
            } else {
                moduleInfoClass.get().removeModuleVersion();
                modified = true;
            }
        }

        List<Options.Requires> requiresToAdd = options.requiresToAdd;
        if (!requiresToAdd.isEmpty()) {
            Optional<ModuleInfoClass> finalModuleInfoClass = moduleInfoClass;
            requiresToAdd.forEach(requires -> {

                EnumSet<RequiresFlag> flags = EnumSet.noneOf(RequiresFlag.class);
                switch (requires.modifier) {
                    case NONE: break;
                    case STATIC:
                        flags.add(RequiresFlag.STATIC_PHASE);
                        break;
                    case TRANSITIVE:
                        flags.add(RequiresFlag.TRANSITIVE);
                        break;
                    default:
                        throw new IllegalStateException("Unknown enum value: " + requires.modifier);
                }

                finalModuleInfoClass.get().setRequires(requires.moduleName, flags, requires.version);
            });

            modified = true;
        }

        List<String> modulesToRemove = options.requiresToRemove;
        if (!modulesToRemove.isEmpty()) {
            modulesToRemove.forEach(moduleInfoClass.get()::removeRequires);
            modified = true;
        }

        if (modified) {
            Path directory = uncheckIO(() -> Files.createTempDirectory("module-info"));
            try {
                Path toPath = directory.resolve("module-info.class");
                try {
                    Output output = new Output();
                    moduleInfoClass.get().appendTo(output);

                    output.writeTo(toPath);

                    jar.updateModuleInfoClass(toPath, jar.path());
                } finally {
                    uncheckIO(() -> Files.delete(toPath));
                }
            } finally {
                uncheckIO(() -> Files.delete(directory));
            }
        }
    }

}
