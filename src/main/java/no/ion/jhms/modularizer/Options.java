package no.ion.jhms.modularizer;

import java.lang.module.ModuleDescriptor;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

public class Options {
    private Path path = null;
    private Action action = Action.NONE;
    private List<RequiresDirective> requiresDirectives = new ArrayList<>();
    private boolean help = false;
    private Optional<ModuleDescriptor.Version> moduleVersion = null;
    private List<String> removeRequiresModules = new ArrayList<>();

    public void setFile(String path) { this.path = Path.of(path); }
    public Path file() { return path; }

    public void addRequires(RequiresDirective requiresDirective) {
        if (requiresDirective.moduleName().equals("java.base")) {
                throw new UserErrorException("error: java.base cannot be removed");
        }
        requiresDirectives.add(requiresDirective);
    }

    public List<RequiresDirective> requiresDirectives() { return requiresDirectives; }

    public void removeRequires(String module) {
        var pointer = new UnicodePointer(module);
        Optional<String> moduleName = pointer.skipModuleName();
        pointer.skipWhitespace();
        if (moduleName.isEmpty() || !pointer.eof()) {
            throw new UserErrorException("error: bad module name: " + module);
        }

        if (moduleName.get().equals("java.base")) {
            throw new UserErrorException("error: java.base cannot be removed");
        }

        this.removeRequiresModules.add(moduleName.get());
    }

    public List<String> removeRequiresModules() { return removeRequiresModules; }

    public void addExports(String exportsSpec) {
    }

    public void removeExports(String packageName) {
    }

    public void setHelp(boolean help) {
        this.help = help;
    }

    public boolean help() { return help;}

    public void setModuleVersion(String rawModuleVersion) {
        this.moduleVersion = rawModuleVersion.isEmpty() ? Optional.empty() :
                Optional.of(ModuleDescriptor.Version.parse(rawModuleVersion));
    }

    /** null means not set, empty means remove, otherwise set version. */
    public Optional<ModuleDescriptor.Version> moduleVersion() { return moduleVersion; }

    public enum Action {
        NONE(""),
        UPDATE("--update"),
        DESCRIBE("--describe-module");

        private final String longOption;

        Action(String longOption) {
            this.longOption = longOption;
        }

        public String longOption() {
            return longOption;
        }
    }

    public Action action() { return action; }
    public void setAction(Action action) { this.action = action; }

    public static class RequiresDirective {
        private final String moduleName;
        private final EnumSet<RequiresFlag> flags;
        private final Optional<ModuleDescriptor.Version> version;

        public RequiresDirective(String moduleName, EnumSet<RequiresFlag> flags,
                                 Optional<ModuleDescriptor.Version> version) {
            this.moduleName = moduleName;
            this.flags = EnumSet.copyOf(flags);
            this.version = version;
        }

        public String moduleName() { return moduleName; }
        public EnumSet<RequiresFlag> flags() { return flags; }
        public Optional<ModuleDescriptor.Version> version() { return version; }
    }

    public static class ExportsDirective {
        private final String packageName;
        private final List<String> targetModules;

        public ExportsDirective(String packageName, List<String> targetModules) {
            this.packageName = packageName;
            this.targetModules = targetModules;
        }

        public String packageName() { return packageName; }
        public List<String> targetModules() { return targetModules; }
    }
}
