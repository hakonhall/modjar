package no.ion.jhms.modularizer;

import java.util.Objects;

public class ConstantPackageInfo extends ConstantPoolEntry {
    private final int nameIndex;

    public ConstantPackageInfo(int nameIndex) {
        super(Constant.Package);
        this.nameIndex = nameIndex;
    }

    public int nameIndex() { return nameIndex; }

    @Override
    protected void entrySpecificAppendTo(Output output) {
        output.appendU2(nameIndex);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConstantPackageInfo that = (ConstantPackageInfo) o;
        return kind() == that.kind() && nameIndex == that.nameIndex;
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind(), nameIndex);
    }
}
