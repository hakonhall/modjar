package no.ion.jhms.modularizer;

import org.junit.jupiter.api.Test;

import java.lang.module.ModuleDescriptor;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class OptionsParserTest {
    @Test
    void verifyBasicAddExports() {
        Main.Options options = OptionsParser.parse("--add-exports", "pack.age");
        List<ModuleUpdater.Options.Exports> exportsToAdd = options.updateOptions().exportsToAdd();
        assertEquals(1, exportsToAdd.size());
        var exports0 = exportsToAdd.get(0);
        assertEquals("pack.age", exports0.packageName());
        assertEquals(0, exports0.moduleTargets().size());
    }

    @Test
    void verifyAdvancedAddExports() {
        Main.Options options = OptionsParser.parse("--add-exports", "pack.age to module.one, module.two");
        List<ModuleUpdater.Options.Exports> exportsToAdd = options.updateOptions().exportsToAdd();
        assertEquals(1, exportsToAdd.size());
        var exports0 = exportsToAdd.get(0);
        assertEquals("pack.age", exports0.packageName());
        List<String> targets = exports0.moduleTargets();
        assertEquals(2, targets.size());
        assertEquals("module.one", targets.get(0));
        assertEquals("module.two", targets.get(1));
    }

    @Test
    void verifyBasicAddRequires() {
        Main.Options options = OptionsParser.parse("--add-requires", "module.name");
        List<ModuleUpdater.Options.Requires> requiresToAdd = options.updateOptions().requiresToAdd();
        assertEquals(1, requiresToAdd.size());
        ModuleUpdater.Options.Requires requires0 = requiresToAdd.get(0);
        assertEquals("module.name", requires0.moduleName());
        assertEquals(ModuleUpdater.Options.Requires.Modifier.NONE, requires0.modifier());
        assertEquals(Optional.empty(), requires0.version());
    }

    @Test
    void verifyAdvancedAddRequires() {
        Main.Options options = OptionsParser.parse("--add-requires", "transitive module.name@2.3.4");
        List<ModuleUpdater.Options.Requires> requires = options.updateOptions().requiresToAdd();
        assertEquals(1, requires.size());
        ModuleUpdater.Options.Requires requires0 = requires.get(0);
        assertEquals("module.name", requires0.moduleName());
        assertEquals(ModuleUpdater.Options.Requires.Modifier.TRANSITIVE, requires0.modifier());
        assertEquals(Optional.of(ModuleDescriptor.Version.parse("2.3.4")), requires0.version());
    }

    @Test
    void testDescribe() {
        var options = OptionsParser.parse("--describe-module");
        assertEquals(Main.Options.Action.DESCRIBE, options.action());
    }

    @Test
    void verifyFileOption() {
        var options = OptionsParser.parse("--file", "somepath");
        assertEquals("somepath", options.updateOptions().jarPath().toString());
        assertEquals("somepath", options.describeOptions().jarPath().toString());
    }

    @Test
    void verifyMainClass() {
        var options = OptionsParser.parse("--main-class", "org.main.clazz");
        assertEquals(Optional.of("org.main.clazz"), options.updateOptions().mainClass());
    }

    @Test
    void verifyRemovalOfMainClass() {
        var options = OptionsParser.parse("--main-class", "");
        assertEquals(Optional.empty(), options.updateOptions().mainClass());
    }

    @Test
    void verifyModuleName() {
        var options = OptionsParser.parse("--module", "module.name");
        assertEquals("module.name", options.updateOptions().moduleName());
    }

    @Test
    void verifyModuleNameAndVersion() {
        var options = OptionsParser.parse("--module", "module.name@1.2.3");
        assertEquals("module.name", options.updateOptions().moduleName());
        assertEquals(Optional.of(ModuleDescriptor.Version.parse("1.2.3")), options.updateOptions().version());
    }

    @Test
    void verifyModuleNameWithEmptyVersion() {
        var options = OptionsParser.parse("--module", "module.name@");
        assertEquals("module.name", options.updateOptions().moduleName());
        assertEquals(Optional.empty(), options.updateOptions().version());
    }

    @Test
    void verifyModuleVersion() {
        var options = OptionsParser.parse("--module-version", "2.3.4");
        assertEquals(Optional.of(ModuleDescriptor.Version.parse("2.3.4")), options.updateOptions().version());
    }

    @Test
    void verifyVersionRemoval() {
        var options = OptionsParser.parse("--module-version", "");
        assertEquals(Optional.empty(), options.updateOptions().version());
    }

    @Test
    void verifyRemoveExports() {
        var options = OptionsParser.parse("--remove-exports", "pack.age");
        List<String> exportsToRemove = options.updateOptions().exportsToRemove();
        assertEquals(1, exportsToRemove.size());
        var exports0 = exportsToRemove.get(0);
        assertEquals("pack.age", exports0);
    }

    @Test
    void verifyRemoveRequires() {
        var options = OptionsParser.parse("--remove-requires", "module.name");
        List<String> requiresToRemove = options.updateOptions().requiresToRemove();
        assertEquals(1, requiresToRemove.size());
        var requires0 = requiresToRemove.get(0);
        assertEquals("module.name", requires0);
    }

    @Test
    void verifyUpdate() {
        var options = OptionsParser.parse("--update");
        assertEquals(Main.Options.Action.UPDATE, options.action());
    }
}