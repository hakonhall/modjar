package no.ion.jhms.modularizer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class ModuleUpdaterTest {
    private final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    private final PrintStream printStream = new PrintStream(baos);
    private Path path;

    @TempDir
    Path tmpDirectory;

    @BeforeEach
    void setUp() {
        path = tmpDirectory.resolve("sample.jar");
    }

    @Test
    public void verifyUpdate() throws IOException {
        Files.copy(Path.of("sample-jar/sample.jar"), path);

        var describeOptions = new ModuleDescription.Options().setJarPath(path);
        try {
            ModuleDescription.describeModule(printStream, describeOptions);
            fail();
        } catch (ErrorException e) {
            assertTrue(e.getMessage().startsWith("error: no module declaration found:"), e::getMessage);
            assertTrue(e.getMessage().endsWith("/sample.jar"));
        }
        assertEquals("", output());

        var updateOptions = new ModuleUpdater.Options()
                .setJarPath(path)
                .setModuleName("module.name");
        ModuleUpdater.update(printStream, updateOptions);
        assertEquals("", output());
    }

    private String getDeclaration() {

    }

    private String output() {
        return baos.toString(StandardCharsets.UTF_8);
    }
}
