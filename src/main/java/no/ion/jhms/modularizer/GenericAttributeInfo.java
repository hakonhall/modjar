package no.ion.jhms.modularizer;

public class GenericAttributeInfo extends AttributeInfo {
    private final String attributeName;
    private final byte[] bytes;
    private final int offset;

    /** @param offset after reading attribute_length. */
    public GenericAttributeInfo(int attributeNameIndex, String attributeName, int attributeLength,
                                byte[] bytes, int offset) {
        super(attributeNameIndex, attributeLength);
        this.attributeName = attributeName;
        this.bytes = bytes;
        this.offset = offset;
    }

    @Override
    protected void attributeSpecificAppendTo(Output output) {
        output.writeByteArray(bytes, offset, originalAttributeLength());
    }
}
