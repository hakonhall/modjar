package no.ion.jhms.modularizer;

import java.io.PrintStream;
import java.lang.module.ModuleDescriptor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static no.ion.jhms.modularizer.Exceptions.uncheckIO;

public class ModuleUpdater {
    private final PrintStream out;
    private final Options options;

    public static void go(PrintStream out, Options options) {
        new ModuleUpdater(out, options).go();
    }

    public ModuleUpdater(PrintStream out, Options options) {
        this.out = out;
        this.options = options;
    }

    private void go() {
        if (options.help()) {
            out.println(Main.helpText());
            return;
        }

        if (options.file() == null) {
            throw new ErrorException("error: no file specified");
        } else if (!Files.isRegularFile(options.file())) {
            throw new ErrorException("error: no such file: " + options.file());
        } else {
            if (!isJarFile() && !isModuleInfoClassFile()) {
                throw new ErrorException("error: file neither a *.jar file nor module-info.class");
            }
        }

        switch (options.action()) {
            case DESCRIBE:
                describeModule();
                break;
            case UPDATE:
                update();
                break;
            case NONE:
                throw new ErrorException("error: nothing to do");
            default:
                throw new ErrorException("error: unknown action: " + options.action());
        }
    }

    private boolean isModuleInfoClassFile() { return options.file().getFileName().toString().equals("module-info.class"); }
    private boolean isJarFile() { return options.file().getFileName().toString().endsWith(".jar"); }

    private void describeModule() {
        final ModuleInfoClass moduleInfoClass;
        if (isJarFile()) {
            Jar jar = new Jar(options.file(), out);
            Optional<ModuleInfoClass> jarModuleInfoClass = jar.readModuleInfoClass();
            if (jarModuleInfoClass.isPresent()) {
                moduleInfoClass = jarModuleInfoClass.get();
            } else {
                throw new ErrorException("error: no module-info.class in " + options.file());
            }
        } else if (isModuleInfoClassFile()) {
            ModuleInfoClassReader reader = ModuleInfoClassReader.open(options.file());
            moduleInfoClass = reader.parse();
        } else {
            throw new ErrorException("error: file neither *.jar nor module-info.class");
        }

        ModuleDescriptor descriptor = new ModuleDescriptorFactory(moduleInfoClass).make();
        ModuleInfoPrinter printer = new ModuleInfoPrinter(descriptor);
        out.print(printer.getModuleInfoJava());
    }

    private void update() {
        final ModuleInfoClass moduleInfoClass;
        Jar jar = null;
        if (isJarFile()) {
            jar = new Jar(options.file(), out);
            Optional<ModuleInfoClass> jarModuleInfoClass = jar.readModuleInfoClass();
            if (jarModuleInfoClass.isPresent()) {
                moduleInfoClass = jarModuleInfoClass.get();
            } else {
                throw new ErrorException("error: no module-info.class in " + options.file());
            }
        } else if (isModuleInfoClassFile()) {
            moduleInfoClass = ModuleInfoClassReader.open(options.file()).parse();
        } else {
            throw new ErrorException("error: file neither *.jar nor module-info.class");
        }

        boolean modified = false;

        if (options.moduleVersion() != null) {
            if (options.moduleVersion().isPresent()) {
                moduleInfoClass.setModuleVersion(options.moduleVersion().get());
                modified = true;
            } else {
                moduleInfoClass.removeModuleVersion();
                modified = true;
            }
        }

        List<Options.RequiresDirective> requiresDirectives = options.requiresDirectives();
        if (!requiresDirectives.isEmpty()) {
            requiresDirectives.forEach(requiresDirective ->
                    moduleInfoClass.setRequires(requiresDirective.moduleName(), requiresDirective.flags(),
                            requiresDirective.version()));
            modified = true;
        }

        List<String> modulesToRemove = options.removeRequiresModules();
        if (!modulesToRemove.isEmpty()) {
            modulesToRemove.forEach(moduleInfoClass::removeRequires);
            modified = true;
        }

        if (modified) {
            Path toPath;
            if (jar == null) {
                toPath = options.file();
            } else {
                Path directory = uncheckIO(() -> Files.createTempDirectory("module-info"));
                toPath = directory.resolve("module-info.class");
            }

            Output output = new Output();
            moduleInfoClass.appendTo(output);

            output.writeTo(toPath);

            if (jar != null) {
                jar.updateModuleInfoClass(toPath, jar.path());
                uncheckIO(() -> Files.delete(toPath));
                uncheckIO(() -> Files.delete(toPath.getParent()));
            }
        }
    }
}
