package no.ion.jhms.modularizer;

import java.lang.module.ModuleDescriptor;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Creates a ModuleDescriptor from a ModuleInfoClass. */
public class ModuleDescriptorFactory {
    private final ModuleInfoClass moduleInfoClass;
    private final ConstantPool constantPool;
    private final ModuleAttribute moduleAttribute;

    public ModuleDescriptorFactory(ModuleInfoClass moduleInfoClass) {
        this.moduleInfoClass = moduleInfoClass;
        this.constantPool = moduleInfoClass.getConstantPool();
        this.moduleAttribute = moduleInfoClass.getAttributeInfos().getModuleAttribute();
    }

    public ModuleDescriptor make() {
        int moduleNameIndex = moduleAttribute.moduleNameIndex();
        String moduleName = constantPool.resolveModuleName(moduleNameIndex);

        EnumSet<ModuleDescriptor.Modifier> moduleModifiers = moduleAttribute.getModuleModifiers();

        ModuleDescriptor.Builder builder = ModuleDescriptor.newModule(moduleName, moduleModifiers);

        int moduleVersionIndex = moduleAttribute.moduleVersionIndex();
        if (moduleVersionIndex != 0) {
            String rawVersion = constantPool.resolveUtf8(moduleVersionIndex);
            builder.version(rawVersion);
        }

        moduleAttribute.requires().forEach(requires -> {
            String requiresModuleName = constantPool.resolveModuleName(requires.requiresIndex());

            var modifiers = EnumSet.noneOf(ModuleDescriptor.Requires.Modifier.class);
            int requiresFlags = requires.requiresFlags();
            if ((requiresFlags & 0x0020) != 0) modifiers.add(ModuleDescriptor.Requires.Modifier.TRANSITIVE);
            if ((requiresFlags & 0x0040) != 0) modifiers.add(ModuleDescriptor.Requires.Modifier.STATIC);
            if ((requiresFlags & 0x1000) != 0) modifiers.add(ModuleDescriptor.Requires.Modifier.SYNTHETIC);
            if ((requiresFlags & 0x8000) != 0) modifiers.add(ModuleDescriptor.Requires.Modifier.MANDATED);

            int versionIndex = requires.requiresVersionIndex();
            if (versionIndex == 0) {
                builder.requires(modifiers, requiresModuleName);
            } else {
                String rawVersion = constantPool.resolveUtf8(versionIndex);
                // NB! The only way to add a requires with version to builder is through Version,
                // which will not work when raw version is unparseable.
                ModuleDescriptor.Version version = ModuleDescriptor.Version.parse(rawVersion);
                builder.requires(modifiers, requiresModuleName, version);
            }
        });

        moduleAttribute.exports().forEach(exports -> {
            String packageName = constantPool.resolvePackageName(exports.index());

            EnumSet<ModuleDescriptor.Exports.Modifier> modifiers = EnumSet.noneOf(ModuleDescriptor.Exports.Modifier.class);
            int flags = exports.flags();
            if ((flags & 0x1000) != 0) modifiers.add(ModuleDescriptor.Exports.Modifier.SYNTHETIC);
            if ((flags & 0x8000) != 0) modifiers.add(ModuleDescriptor.Exports.Modifier.MANDATED);

            List<Integer> toIndices = exports.toIndices();
            if (toIndices.isEmpty()) {
                builder.exports(modifiers, packageName);
            } else {
                Set<String> targets = toIndices.stream()
                        .map(constantPool::resolveModuleName)
                        .collect(Collectors.toSet());

                builder.exports(modifiers, packageName, targets);
            }
        });

        moduleAttribute.opens().forEach(opens -> {
            String packageName = constantPool.resolvePackageName(opens.index());

            EnumSet<ModuleDescriptor.Opens.Modifier> modifiers = EnumSet.noneOf(ModuleDescriptor.Opens.Modifier.class);
            int flags = opens.flags();
            if ((flags & 0x1000) != 0) modifiers.add(ModuleDescriptor.Opens.Modifier.SYNTHETIC);
            if ((flags & 0x8000) != 0) modifiers.add(ModuleDescriptor.Opens.Modifier.MANDATED);

            List<Integer> toIndices = opens.toIndices();
            if (toIndices.isEmpty()) {
                builder.opens(modifiers, packageName);
            } else {
                Set<String> targets = toIndices.stream()
                        .map(constantPool::resolveModuleName)
                        .collect(Collectors.toSet());

                builder.opens(modifiers, packageName, targets);
            }
        });

        moduleAttribute.usesIndices().forEach(usesIndex -> {
            builder.uses(constantPool.resolveClassName(usesIndex));
        });

        moduleAttribute.provides().forEach(provides -> {
            String serviceInterface = constantPool.resolveClassName(provides.providesIndex());
            List<String> providers = provides.providesWithIndices().stream()
                    .map(constantPool::resolveClassName)
                    .collect(Collectors.toList());

            builder.provides(serviceInterface, providers);
        });

        return builder.build();
    }
}
