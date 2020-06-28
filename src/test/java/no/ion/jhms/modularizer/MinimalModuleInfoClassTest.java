package no.ion.jhms.modularizer;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.module.ModuleDescriptor;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MinimalModuleInfoClassTest {
    // Made made by javac, see min-descriptor/Makefile.
    private final Path path = Path.of("src/test/resources/min-descriptor/module-info.class");

    @Test
    void verifyMinimalModuleInfoMatchesTheOneMadeByJavac() throws IOException {
        ModuleInfoClassReader reader = ModuleInfoClassReader.open(path);
        ModuleInfoClass expectedModuleInfoClass = reader.parse();
        ModuleDescriptor expectedModuleDescriptor = new ModuleDescriptorFactory(expectedModuleInfoClass).make();
        ModuleDescriptor.Requires expectedJavaBaseRequires = expectedModuleDescriptor.requires().stream()
                .filter(requires -> requires.name().equals("java.base"))
                .findAny()
                .orElseThrow();

        ModuleInfoClass actualModuleInfoClass = MinimalModuleInfoClass.create(
                expectedModuleInfoClass.minorVersion(),
                expectedModuleInfoClass.majorVersion(),
                expectedJavaBaseRequires.rawCompiledVersion().orElseThrow(),
                "MODULENAME");

        Output output = new Output();
        actualModuleInfoClass.appendTo(output);

        byte[] expected = Files.readAllBytes(path);
        byte[] actual = output.toByteArray();

        for (int i = 0; i < expected.length && i < actual.length; ++i) {
            assertEquals(expected[i], actual[i], "For i = " + i);
        }

        assertEquals(expected.length, actual.length);
    }

    @Test
    void verifyMainCreate() {
        MinimalModuleInfoClass.create("MODULE.NAME");
    }
}