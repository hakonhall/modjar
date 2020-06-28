package no.ion.jhms.modularizer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ModuleInfoClassReader {

    private final byte[] bytes;
    private int offset = 0;

    public static ModuleInfoClassReader open(Path moduleInfoClassPath) {
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(moduleInfoClassPath);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return openWith(bytes);
    }

    public static ModuleInfoClassReader openWith(byte[] bytes) {
        return new ModuleInfoClassReader(bytes).initialParse();
    }

    public static ModuleInfoClass disassemble(byte[] bytes) {
        return openWith(bytes).parse();
    }

    public ModuleInfoClassReader(byte[] bytes) {
        this.bytes = bytes;
    }

    private ModuleInfoClassReader initialParse() {
        return this;
    }

    public ModuleInfoClass parse() {
        int magic = readU4();
        if (magic != ModuleInfoClass.MAGIC) {
            throw new BadModuleInfoException("Does not start with magic " + ModuleInfoClass.MAGIC);
        }

        int minorVersion = readU2();
        int majorVersion = readU2();
        ConstantPool constantPool = constantPool();

        int accessFlag = readU2();
        if (accessFlag != ModuleInfoClass.ACC_MODULE) {
            throw new BadModuleInfoException("Access flag not a module: " + accessFlag);
        }

        int thisClass = readU2();
        String binaryClassName = constantPool.resolveClassName(thisClass);
        if (!binaryClassName.equals("module-info")) {
            throw new BadModuleInfoException("The class name of a module must be module-info: " + binaryClassName);
        }

        int superClass = readU2();
        if (superClass != 0) {
            throw new BadModuleInfoException("super_class not 0: " + superClass);
        }

        int interfacesCount = readU2();
        if (interfacesCount != 0) {
            throw new BadModuleInfoException("interfaces_count not 0: " + interfacesCount);
        }

        int fieldsCount = readU2();
        if (fieldsCount != 0) {
            throw new BadModuleInfoException("fields_count not 0: " + fieldsCount);
        }

        int methodsCount = readU2();
        if (methodsCount != 0) {
            throw new BadModuleInfoException("methods_count not 0: " + methodsCount);
        }

        AttributeInfos attributes = readAttributeInfos(constantPool);

        return new ModuleInfoClass(magic, minorVersion, majorVersion, constantPool, accessFlag, thisClass, attributes);
    }

    private AttributeInfos readAttributeInfos(ConstantPool constantPool) {
        int attributesCount = readU2();

        AttributeInfos attributeInfos = new AttributeInfos();

        for (int index = 0; index < attributesCount; ++index) {
            int attributeNameIndex = readU2();
            int attributeLength = readU4();

            String attributeName = constantPool.resolveUtf8(attributeNameIndex);
            switch (attributeName) {
                case "Module":
                    attributeInfos.addModuleAttribute(readModuleAttribute(attributeNameIndex, attributeLength));
                    break;
                case "SourceFile":
                    attributeInfos.addSourceFile(new SourceFileAttribute(attributeNameIndex, attributeLength, readU2()));
                    break;
                case "ModulePackages":
                case "ModuleMainClass":
                case "InnerClasses":
                case "SourceDebugExtension":
                case "RuntimeVisibleAnnotations":
                case "RuntimeInvisibleAnnotations":
                    attributeInfos.add(new GenericAttributeInfo(attributeNameIndex, attributeName, attributeLength, bytes, offset));
                    offset += attributeLength;
                    break;
                default:
                    throw new BadModuleInfoException("attribute_info not supported for module: " + attributeName);
            }
        }

        attributeInfos.validate();

        return attributeInfos;
    }

    private ModuleAttribute readModuleAttribute(int attributeNameIndex, int attributeLength) {
        int moduleNameIndex = readU2();
        int moduleFlags = readU2();
        int moduleVersionIndex = readU2();

        var moduleAttribute = new ModuleAttribute(attributeNameIndex, attributeLength, moduleNameIndex, moduleFlags,
                moduleVersionIndex);

        int requiresCount = readU2();
        for (int index = 0; index < requiresCount; ++index) {
            int requiresIndex = readU2();
            int requiresFlags = readU2();
            int requiresVersionIndex = readU2();
            moduleAttribute.addRequires(new Requires(requiresIndex, requiresFlags, requiresVersionIndex));
        }

        int exportsCount = readU2();
        for (int index = 0; index < exportsCount; ++index) {
            int exportsIndex = readU2();
            int exportsFlags = readU2();
            var exports = new Exports(exportsIndex, exportsFlags);
            int exportsToCount = readU2();
            for (int j = 0; j < exportsToCount; ++j) {
                exports.addTo(readU2());
            }

            moduleAttribute.addExports(exports);
        }

        int opensCount = readU2();
        for (int index = 0; index < opensCount; ++index) {
            int opensIndex = readU2();
            int opensFlags = readU2();
            Opens opens = new Opens(opensIndex, opensFlags);
            int opensToCount = readU2();
            for (int j = 0; j < opensToCount; ++j) {
                opens.addToIndex(readU2());
            }

            moduleAttribute.addOpens(opens);
        }

        int usesCount = readU2();
        for (int index = 0; index < usesCount; ++index) {
            moduleAttribute.addUses(readU2());
        }

        int providesCount = readU2();
        for (int index = 0; index < providesCount; ++index) {
            int providesIndex = readU2();
            var provides = new Provides(providesIndex);
            int providesWithCount = readU2();
            for (int j = 0; j < providesWithCount; ++j) {
                provides.addWithIndex(readU2());
            }

            moduleAttribute.addProvides(provides);
        }

        return moduleAttribute;
    }

    private ConstantPool constantPool() {
        int constantPoolCount = readU2();
        ConstantPool constantPool = new ConstantPool();

        for (int index = 1; index < constantPoolCount; ) {
            int tag = readU1();
            Constant constant = Constant.fromTag(tag);

            switch (constant) {
                case Utf8:
                    constantPool.append(index, readConstantUtf8());
                    index += 1;
                    break;
                case Class:
                    constantPool.append(index, readConstantClassInfo());
                    index += 1;
                    break;
                case Module:
                    constantPool.append(index, readConstantModule());
                    index += 1;
                    break;
                case Package:
                    constantPool.append(index, readConstantPackageInfo());
                    index += 1;
                    break;

                case Integer:
                case Float:
                case Long:
                case Double:
                case String:
                case Fieldref:
                case Methodref:
                case InterfaceMethodref:
                case NameAndType:
                case MethodHandle:
                case MethodType:
                case Dynamic:
                case InvokeDynamic:
                    throw new UnsupportedOperationException("Constant pool tag: " + tag);
                default:
                    throw new BadModuleInfoException("Unknown constant pool tag at " + index + ": " + tag);
            }
        }

        return constantPool;
    }

    private ConstantPackageInfo readConstantPackageInfo() {
        return new ConstantPackageInfo(readU2());
    }

    private ConstantModuleInfo readConstantModule() {
        return new ConstantModuleInfo(readU2());
    }

    private ConstantUtf8 readConstantUtf8() {
        int length = readU2();
        int start = offset;
        offset += length;
        return new ConstantUtf8(bytes, start, offset);
    }

    private ConstantClassInfo readConstantClassInfo() {
        int nameIndex = readU2();
        return new ConstantClassInfo(nameIndex);
    }

    private int readU4() { return (readU2() << 16) | readU2(); }
    private int readU2() { return (readU1() << 8) | readU1(); }
    private int readU1() { return 0xFF & bytes[offset++]; }

    private void assertIndex(int index) {
        if (index >= bytes.length) {
            throw new IndexOutOfBoundsException(index);
        }
    }

}
