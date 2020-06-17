package no.ion.jhms.modularizer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.module.ModuleDescriptor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SerializationTest {
    private final Path path = Path.of("src/test/resources/module-info/module-info.class");

    @TempDir
    Path directory;

    @Test void verifySerialization() throws IOException {
        ModuleInfoClassReader reader = ModuleInfoClassReader.open(path);
        ModuleInfoClass moduleInfoClass = reader.parse();

        Output output = new Output();
        moduleInfoClass.appendTo(output);

        byte[] expected = Files.readAllBytes(path);
        byte[] actual = output.toByteArray();

        for (int i = 0; i < expected.length && i < actual.length; ++i) {
            assertEquals(expected[i], actual[i], "For i = " + i);
        }

        assertEquals(expected.length, actual.length);
    }

    @Test void writeModifiedAndReread() throws IOException {
        assertTrue(Files.isDirectory(directory));

        var reader = ModuleInfoClassReader.open(path);
        var moduleInfoClass = reader.parse();

        assertEquals(Optional.of(ModuleDescriptor.Version.parse("1.2.3")), moduleInfoClass.getModuleVersion());
        moduleInfoClass.setModuleVersion(ModuleDescriptor.Version.parse("2.3.4"));

        Path updatedPath = Path.of("target/SerialzationTest/module-info.class");
        Files.createDirectories(updatedPath.getParent());

        Output output = new Output();
        moduleInfoClass.appendTo(output);
        output.writeTo(updatedPath);

        ModuleInfoClass updatedModuleInfoClass = ModuleInfoClassReader.open(updatedPath).parse();
        Optional<ModuleDescriptor.Version> updatedVersion = updatedModuleInfoClass.getModuleVersion();
        assertEquals(Optional.of(ModuleDescriptor.Version.parse("2.3.4")), updatedVersion);

    }
}
