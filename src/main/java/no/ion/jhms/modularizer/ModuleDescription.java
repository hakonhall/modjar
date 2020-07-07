package no.ion.jhms.modularizer;

import java.io.PrintStream;
import java.lang.module.ModuleDescriptor;
import java.nio.file.Path;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public class ModuleDescription {
    private final PrintStream out;
    private final Options options;

    public static class Options {
        private Path jarPath = null;
        private PrintStream out = System.out;

        public Options() {}

        public Options setJarPath(Path path) {
            this.jarPath = requireNonNull(path);
            return this;
        }

        public Path jarPath() { return jarPath; }
        public PrintStream out() { return out; }
    }

    /**
     * Writes a description of the modular JAR given by {@link Options#jarPath()}.
     *
     * @throws ErrorException if jar file is not a module
     */
    public static void describeModule(PrintStream out, Options options) {
        new ModuleDescription(out, options).describe();
    }

    private boolean isModuleInfoClassFile() { return options.jarPath.getFileName().toString().equals("module-info.class"); }
    private boolean isJarFile() { return options.jarPath.getFileName().toString().endsWith(".jar"); }

    private ModuleDescription(PrintStream out, Options options) {
        this.out = out;
        this.options = options;
    }

    private void describe() {
        if (options.jarPath() == null) {
            throw new ErrorException("missing JAR path");
        }

        final ModuleInfoClass moduleInfoClass;
        if (isJarFile()) {
            Jar jar = new Jar(options.jarPath(), out);
            Optional<ModuleInfoClass> jarModuleInfoClass = jar.readModuleInfoClass();
            if (jarModuleInfoClass.isPresent()) {
                moduleInfoClass = jarModuleInfoClass.get();
            } else {
                throw new ErrorException("no module declaration found: " + options.jarPath());
            }
        } else if (isModuleInfoClassFile()) {
            ModuleInfoClassReader reader = ModuleInfoClassReader.open(options.jarPath());
            moduleInfoClass = reader.parse();
        } else {
            throw new ErrorException("file neither *.jar nor module-info.class");
        }

        ModuleDescriptor descriptor = new ModuleDescriptorFactory(moduleInfoClass).make();
        ModuleInfoPrinter printer = new ModuleInfoPrinter(descriptor);
        options.out().print(printer.getModuleInfoJava());
    }
}
