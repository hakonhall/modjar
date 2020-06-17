package no.ion.jhms.modularizer;

public abstract class ConstantPoolEntry {
    private final Constant constant;

    protected ConstantPoolEntry(Constant constant) {
        this.constant = constant;
    }

    public Constant kind() { return constant; }

    /** Serialize entry to output.
     * @param output*/
    public final void appendTo(Output output) {
        output.appendU1(constant.tag());
        entrySpecificAppendTo(output);
    }

    protected abstract void entrySpecificAppendTo(Output output);
}
