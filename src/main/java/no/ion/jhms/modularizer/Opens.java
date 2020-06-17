package no.ion.jhms.modularizer;

import java.util.ArrayList;
import java.util.List;

public class Opens {
    private final int index;
    private final int flags;
    private final List<Integer> toIndices = new ArrayList<>();

    public Opens(int index, int flags) {
        this.index = index;
        this.flags = flags;
    }

    public void addToIndex(int opensToIndex) { toIndices.add(opensToIndex); }

    public int index() { return index; }
    public int flags() { return flags; }
    public List<Integer> toIndices() { return toIndices; }

    public void appendTo(Output output) {
        output.appendU2(index);
        output.appendU2(flags);
        output.appendU2(toIndices.size());
        toIndices.forEach(output::appendU2);
    }
}
