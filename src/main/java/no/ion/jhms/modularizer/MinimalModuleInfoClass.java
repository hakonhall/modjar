package no.ion.jhms.modularizer;

public class MinimalModuleInfoClass {
    public static ModuleInfoClass create(String moduleName) {
        String javaVersion = System.getProperty("java.version");

        String classFileFormatVersion = System.getProperty("java.class.version");
        String[] parts = classFileFormatVersion.split("\\.");
        int majorVersion = Integer.parseInt(parts[0]);
        int minorVersion = Integer.parseInt(parts[1]);

        return create(minorVersion, majorVersion, javaVersion, moduleName);
    }

    /**
     *
     * @param minorVersion    E.g. 0
     * @param majorVersion    E.g. 55 for JVM 11
     * @param javaBaseVersion E.g. "11.0.7"
     * @param moduleName      E.g. "MODULENAME".  It's the caller's responsibility to pass a legal module name.
     */
    public static ModuleInfoClass create(int minorVersion, int majorVersion, String javaBaseVersion, String moduleName) {

        ConstantPool constantPool = new ConstantPool();
        constantPool.add(new ConstantClassInfo(8)); // 1
        constantPool.add(ConstantUtf8.fromString("SourceFile")); // 2
        constantPool.add(ConstantUtf8.fromString("module-info.java")); // 3
        constantPool.add(ConstantUtf8.fromString("Module")); // 4
        constantPool.add(new ConstantModuleInfo(9)); // 5
        constantPool.add(new ConstantModuleInfo((10))); // 6
        constantPool.add(ConstantUtf8.fromString(javaBaseVersion)); // 7
        constantPool.add(ConstantUtf8.fromString("module-info")); // 8
        constantPool.add(ConstantUtf8.fromString(moduleName)); // 9
        constantPool.add(ConstantUtf8.fromString("java.base")); // 10

        AttributeInfos attributeInfos = new AttributeInfos();
        attributeInfos.addSourceFile(new SourceFileAttribute(
                2, // "SourceFile"
                2, // attribute_length
                3)); // "module-info.java"
        var moduleAttribute = new ModuleAttribute(
                4, // "Module"
                -1, // attribute_length, unknown at the moment. The length is dynamically calculated when serializing
                5, // moduleName module info
                0, // no module flags
                0); // no module version
        moduleAttribute.addRequires(new Requires(
                6, // java.base module info
                RequiresFlag.MANDATED.value(),
                7)); // javaBaseVersion
        attributeInfos.addModuleAttribute(moduleAttribute);

        return new ModuleInfoClass(
                ModuleInfoClass.MAGIC, minorVersion, majorVersion, constantPool, ModuleInfoClass.ACC_MODULE,
                1, // "module-info" class info
                attributeInfos);
    }

}
