package no.ion.jhms.modularizer;

import java.util.TreeMap;

public class ConstantPool {
    private final TreeMap<Integer, ConstantPoolEntry> constantPool = new TreeMap<>();

    /** constant_pool_count */
    public int count() {
        return constantPool.size() + 1;
    }

    /** Append a unique entry at the expected index. */
    public void append(int index, ConstantPoolEntry entry) {
        if (index != count()) {
            throw new IllegalArgumentException("Tried to add entry at " + index + " with contant_pool_count: " + count());
        }

        append(entry);
    }

    public void replace(int index, ConstantUtf8 utf8) {
        constantPool.put(index, utf8);
    }

    /** 
     * Append the entry to the constant pool and return its index, unless the entry already exist and in case
     * the existing index is returned. 
     */
    public int add(ConstantPoolEntry entry) {
        for (var pair : constantPool.entrySet()) {
            var index = pair.getKey();
            var currentEntry = pair.getValue();
            
            if (currentEntry.equals(entry)) {
                return index;
            }
        }
        
        return append(entry);
    }

    /** Append a unique entry to the constant pool. */
    private int append(ConstantPoolEntry entry) {
        int index = count();
        constantPool.put(index, entry);
        return index;
    }

    public String resolveUtf8(int index) {
        return getUtf8Entry(index).toString();
    }

    public String resolveClassName(int index) {
        return resolveUtf8(getClassInfoEntry(index).nameIndex()).replace('/','.');
    }

    public String resolvePackageName(int index) {
        return resolveUtf8(getPackageInfoEntry(index).nameIndex()).replace('/', '.');
    }

    public String resolveModuleName(int index) {
        // NB: The module name is not validated.
        return resolveUtf8(getModuleInfoEntry(index).nameIndex());
    }

    private ConstantUtf8 getUtf8Entry(int index) { return getEntry(index, ConstantUtf8.class, Constant.Utf8); }
    private ConstantClassInfo getClassInfoEntry(int index) { return getEntry(index, ConstantClassInfo.class, Constant.Class); }
    private ConstantPackageInfo getPackageInfoEntry(int index) { return getEntry(index, ConstantPackageInfo.class, Constant.Package); }
    private ConstantModuleInfo getModuleInfoEntry(int index) { return getEntry(index, ConstantModuleInfo.class, Constant.Module); }

    private <T extends ConstantPoolEntry> T getEntry(int index, Class<T> clazz, Constant constant) {
        ConstantPoolEntry entry = constantPool.get(index);
        if (entry == null) {
            throw new BadModuleInfoException("There is no constant_pool entry with index " + index);
        }

        if (entry.kind() != constant || !clazz.isInstance(entry)) {
            throw new IllegalArgumentException("Entry at index " + index + " is not " + constant + ": " + entry.kind());
        }

        return clazz.cast(entry);
    }

    public void appendTo(Output output) {
        output.appendU2(count());
        for (var entry : constantPool.values()) {
            entry.appendTo(output);
        }
    }
}
