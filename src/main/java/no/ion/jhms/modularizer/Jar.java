package no.ion.jhms.modularizer;

import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.spi.ToolProvider;

import static java.util.spi.ToolProvider.findFirst;
import static no.ion.jhms.modularizer.Exceptions.uncheckIO;

public class Jar {
    private final Path path;
    private final PrintStream out;
    private final ToolProvider jarToolProvider;

    public Jar(Path path, PrintStream out) {
        this.path = path;
        this.out = out;
        this.jarToolProvider = findFirst("jar")
                .orElseThrow(() -> new ErrorException("error: no jar tool available"));
    }

    public Path path() { return path; }

    public Optional<ModuleInfoClass> readModuleInfoClass() {
        JarFile jarFile = uncheckIO(() -> new JarFile(path.toFile(), false));
        JarEntry moduleInfoClassEntry = jarFile.getJarEntry("module-info.class");
        if (moduleInfoClassEntry == null) return Optional.empty();
        InputStream inputStream = uncheckIO(() -> jarFile.getInputStream(moduleInfoClassEntry));
        byte[] bytes = uncheckIO(inputStream::readAllBytes);
        ModuleInfoClass moduleInfoClass = ModuleInfoClassReader.disassemble(bytes);
        return Optional.of(moduleInfoClass);
    }

    public void updateModuleInfoClass(Path moduleInfoClassPath, Path jarPath) {
        jarToolProvider.run(out, out, "--update", "--file", jarPath.toString(), "-C",
                moduleInfoClassPath.getParent().toString(), moduleInfoClassPath.getFileName().toString());
    }
}
