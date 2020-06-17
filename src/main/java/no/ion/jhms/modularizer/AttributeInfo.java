package no.ion.jhms.modularizer;

public abstract class AttributeInfo {
    private final int attributeNameIndex;
    private final int originalAttributeLength;

    public AttributeInfo(int attributeNameIndex, int attributeLength) {
        this.attributeNameIndex = attributeNameIndex;
        this.originalAttributeLength = attributeLength;
    }

    public int attributeNameIndex() { return attributeNameIndex; }
    public int originalAttributeLength() { return originalAttributeLength; }

    public final void appendTo(Output output) {
        output.appendU2(attributeNameIndex);

        int attributeLengthOffset = output.size();
        output.appendU4(0);

        int infoOffset = output.size();
        attributeSpecificAppendTo(output);
        int attributeLength = output.size() - infoOffset;

        output.writeU4At(attributeLengthOffset, attributeLength);
    }

    protected abstract void attributeSpecificAppendTo(Output output);
}
