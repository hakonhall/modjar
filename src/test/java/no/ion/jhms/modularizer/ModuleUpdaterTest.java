package no.ion.jhms.modularizer;

import org.junit.jupiter.api.Test;

import java.lang.module.ModuleDescriptor;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModuleUpdaterTest {
    @Test
    public void addModuleInfoClass() {
        ModuleInfoClassReader reader = ModuleInfoClassReader.open(Path.of("src/test/resources/min-descriptor/module-info.class"));
        ModuleInfoClass moduleInfoClass = reader.parse();
    }
}
