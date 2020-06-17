package no.ion.jhms.modularizer;

public class SourceFileAttribute extends AttributeInfo {
    private final int sourceFileIndex;

    public SourceFileAttribute(int attributeNameIndex, int attributeLength, int sourceFileIndex) {
        super(attributeNameIndex, attributeLength);
        this.sourceFileIndex = sourceFileIndex;
    }

    @Override
    protected void attributeSpecificAppendTo(Output output) {
        output.appendU2(sourceFileIndex);
    }
}
