package no.ion.jhms.modularizer;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class OutputTest {
    private final FileSystem fileSystem = Jimfs.newFileSystem(Configuration.unix());
    private final Output output = new Output(3);

    @Test
    void verifyOutput() throws IOException {
        output.appendU1(0x01);
        output.appendU2(0x0203);
        output.appendU4(0x04050607);
        output.appendU1(-1);
        output.appendU2(-1);
        output.appendU4(-1);

        Path path = fileSystem.getPath("/foo");
        output.writeTo(path);

        byte[] bytes = Files.readAllBytes(path);
        assertEquals(14, bytes.length);
        assertEquals(1, bytes[0]);

        assertEquals(2, bytes[1]);
        assertEquals(3, bytes[2]);

        assertEquals(4, bytes[3]);
        assertEquals(5, bytes[4]);
        assertEquals(6, bytes[5]);
        assertEquals(7, bytes[6]);

        assertEquals(-1, bytes[7]);

        assertEquals(-1, bytes[8]);
        assertEquals(-1, bytes[9]);

        assertEquals(-1, bytes[10]);
        assertEquals(-1, bytes[11]);
        assertEquals(-1, bytes[12]);
        assertEquals(-1, bytes[13]);
    }
}