package no.ion.jhms.modularizer;

import java.util.stream.Stream;

public enum Constant {
    Utf8(1),
    Class(7),
    Integer(3),
    Float(4),
    Long(5),
    Double(6),
    String(8),
    Fieldref(9),
    Methodref(10),
    InterfaceMethodref(11),
    NameAndType(12),
    MethodHandle(15),
    MethodType(16),
    Dynamic(17),
    InvokeDynamic(18),
    Module(19),
    Package(20);

    private final int tag;

    public static Constant fromTag(int tag) {
        return Stream.of(values()).filter(value -> value.tag == tag).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown tag: " + tag));
    }

    Constant(int tag) {
        this.tag = tag;
    }

    public int tag() { return tag; }
}
