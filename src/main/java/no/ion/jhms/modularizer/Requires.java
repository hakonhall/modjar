package no.ion.jhms.modularizer;

public class Requires {
    private final int requiresIndex;
    private int requiresFlags;
    private int requiresVersionIndex;

    public Requires(int requiresIndex, int requiresFlags, int requiresVersionIndex) {
        this.requiresIndex = requiresIndex;
        this.requiresFlags = requiresFlags;
        this.requiresVersionIndex = requiresVersionIndex;
    }

    public int requiresIndex() { return requiresIndex; }
    public int requiresFlags() { return requiresFlags; }
    public int requiresVersionIndex() { return requiresVersionIndex; }

    public void setFlags(int requiresFlags) {
        this.requiresFlags = requiresFlags;
    }

    public void setRequiresVersionIndex(int index) {
        requiresVersionIndex = index;
    }

    public void appendTo(Output output) {
        output.appendU2(requiresIndex);
        output.appendU2(requiresFlags);
        output.appendU2(requiresVersionIndex);
    }
}
