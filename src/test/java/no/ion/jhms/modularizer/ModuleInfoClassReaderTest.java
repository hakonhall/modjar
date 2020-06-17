package no.ion.jhms.modularizer;

import org.junit.jupiter.api.Test;

import java.lang.module.ModuleDescriptor;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModuleInfoClassReaderTest {
    @Test void verifyReader() {
        ModuleInfoClassReader reader = ModuleInfoClassReader.open(Path.of("src/test/resources/module-info/module-info.class"));
        ModuleInfoClass moduleInfoClass = reader.parse();
        assertEquals(0xCAFEBABE, moduleInfoClass.magic());
        assertTrue(moduleInfoClass.majorVersion() >= 53, "Requires at least Java 9");
        assertTrue(moduleInfoClass.minorVersion() >= 0, "Requires at least Java 9");

        ModuleDescriptor descriptor = new ModuleDescriptorFactory(moduleInfoClass).make();
        ModuleInfoPrinter printer = new ModuleInfoPrinter(descriptor);
        String actualModuleDeclaration = printer.toString();
        assertEquals(
                "module sample { // @1.2.3\n" +
                "\n" +
                "    requires java.base; // @JVMVER mandated\n" +
                "    requires java.compiler; // @JVMVER\n" +
                "\n" +
                "    exports sample.exported;\n" +
                "\n" +
                "    opens sample.internal to\n" +
                "        java.compiler;\n" +
                "\n" +
                "    use javax.tools.Tool;\n" +
                "\n" +
                "    provides java.lang.Object with\n" +
                "        sample.internal.Internal;\n" +
                "\n" +
                "}\n",
                normalizeJvmVersionString(actualModuleDeclaration));
    }

    private static String normalizeJvmVersionString(String moduleInfoJava) {
        Pattern pattern = Pattern.compile("requires java.base; // @([0-9.]+) mandated");
        Matcher matcher = pattern.matcher(moduleInfoJava);
        if (matcher.find()) {
            return moduleInfoJava.replaceAll("@" + matcher.group(1), "@JVMVER");
        } else {
            return moduleInfoJava;
        }
    }
}