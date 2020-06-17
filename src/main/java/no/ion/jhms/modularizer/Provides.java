package no.ion.jhms.modularizer;

import java.util.ArrayList;
import java.util.List;

public class Provides {
    private final int index;
    private final List<Integer> withIndices = new ArrayList<>();

    public Provides(int index) {
        this.index = index;
    }

    public void addWithIndex(int providesWithIndex) { withIndices.add(providesWithIndex); }

    public int providesIndex() { return index; }
    public List<Integer> providesWithIndices() { return withIndices; }

    public void appendTo(Output output) {
        output.appendU2(index);
        output.appendU2(withIndices.size());
        withIndices.forEach(output::appendU2);
    }
}
