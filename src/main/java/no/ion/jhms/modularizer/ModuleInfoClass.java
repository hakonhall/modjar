package no.ion.jhms.modularizer;

import java.lang.module.ModuleDescriptor.Version;
import java.util.EnumSet;
import java.util.Optional;

public class ModuleInfoClass {
    private int magic;
    private int minorVersion;
    private int majorVersion;
    private final ConstantPool constantPool;
    private int accessFlags;
    private int thisClass;
    private final AttributeInfos attributes;

    public ModuleInfoClass(int magic, int minorVersion, int majorVersion, ConstantPool constantPool, int accessFlags,
                           int thisClass, AttributeInfos attributes) {
        this.magic = magic;
        this.minorVersion = minorVersion;
        this.majorVersion = majorVersion;
        this.constantPool = constantPool;
        this.accessFlags = accessFlags;
        this.thisClass = thisClass;
        this.attributes = attributes;
    }

    public int magic() { return magic; }
    public int minorVersion() { return minorVersion; }
    public int majorVersion() { return majorVersion; }
    public ConstantPool getConstantPool() { return constantPool; }
    public AttributeInfos getAttributeInfos() { return attributes; }

    public String getModuleName() {
        int moduleNameIndex = getAttributeInfos().getModuleAttribute().moduleNameIndex();
        return constantPool.resolveModuleName(moduleNameIndex);
    }

    public void appendTo(Output output) {
        output.appendU4(magic);
        output.appendU2(minorVersion);
        output.appendU2(majorVersion);
        constantPool.appendTo(output);
        output.appendU2(accessFlags);
        output.appendU2(thisClass);
        output.appendU2(0); // super_class
        output.appendU2(0); // interfaces_count
        output.appendU2(0); // fields_count
        output.appendU2(0); // methods_count
        attributes.appendTo(output);
    }

    public Optional<Version> getModuleVersion() {
        int moduleVersionIndex = attributes.getModuleAttribute().moduleVersionIndex();
        if (moduleVersionIndex == 0) {
            return Optional.empty();
        } else {
            String rawVersion = constantPool.resolveUtf8(moduleVersionIndex);
            return Optional.of(Version.parse(rawVersion));
        }
    }

    public void setModuleVersion(Version version) {
        ConstantUtf8 utf8 = ConstantUtf8.fromString(version.toString());

        var moduleAttribute = attributes.getModuleAttribute();

        int index = moduleAttribute.moduleVersionIndex();
        if (index == 0) {
            index = constantPool.add(utf8);
            moduleAttribute.setModuleVersionIndex(index);
        } else {
            constantPool.replace(index, utf8);
        }
    }

    public void removeModuleVersion() {
        var moduleAttribute = attributes.getModuleAttribute();

        int index = moduleAttribute.moduleVersionIndex();
        if (index > 0) {
            // TODO: Proper support for removing entries from constant pool.
            // Removing entries from the constant pool requires keeping track of all references
            // to each constant pool entry:
            //   1. Only if reference count reaches 0 is an entry removed.
            //   2. If an entry is removed, all references to the following entries must be decremented.
            //
            // In particular, this requires understanding ALL sections of the class file, which can contain
            // expressions.
            moduleAttribute.setModuleVersionIndex(0);
        }
    }

    public void setRequires(String moduleName, EnumSet<RequiresFlag> flags, Optional<Version> version) {
        int requiresFlags = RequiresFlag.toRequiresFlag(flags);

        ModuleAttribute moduleAttribute = attributes.getModuleAttribute();
        for (var requires : moduleAttribute.requires()) {
            int indexOfModuleName = requires.requiresIndex();
            String requiresModuleName = constantPool.resolveModuleName(indexOfModuleName);
            if (!requiresModuleName.equals(moduleName)) {
                continue;
            }

            requires.setFlags(requiresFlags);

            if (version.isPresent()) {
                // TODO: Ref. count and be able to remove entries in constant pool
                var versionUtf8Entry = ConstantUtf8.fromString(version.get().toString());
                int versionUtf8Index = constantPool.add(versionUtf8Entry);
                requires.setRequiresVersionIndex(versionUtf8Index);
            } else {
                // TODO: Ref. count and be able to remove entry in constant pool
                requires.setRequiresVersionIndex(0);
            }

            return;
        }

        var moduleNameUtf8Entry = ConstantUtf8.fromString(moduleName);
        int moduleNameUtf8Index = constantPool.add(moduleNameUtf8Entry);
        var moduleNameModuleEntry = new ConstantModuleInfo(moduleNameUtf8Index);
        int moduleNameModuleIndex = constantPool.add(moduleNameModuleEntry);

        final int versionUtf8Index;
        if (version.isPresent()) {
            var versionUtf8Entry = ConstantUtf8.fromString(version.get().toString());
            versionUtf8Index = constantPool.add(versionUtf8Entry);
        } else {
            versionUtf8Index = 0;
        }

        Requires requires = new Requires(moduleNameModuleIndex, requiresFlags, versionUtf8Index);
        moduleAttribute.addRequires(requires);
    }

    public void removeRequires(String module) {
        // TODO: Ref. count and be able to remove entries in constant pool
        attributes.getModuleAttribute().removeRequiresIf(requires -> {
            int requiresIndex = requires.requiresIndex();
            String moduleName = constantPool.resolveModuleName(requiresIndex);
            if (moduleName.equals(module)) {
                if (RequiresFlag.MANDATED.hasFlag(requires.requiresFlags())) {
                    throw new ErrorException("error: unable to remove mandated module: " + module);
                }
                return true;
            } else {
                return false;
            }
        });
    }

}
