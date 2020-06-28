package no.ion.jhms.modularizer;

import java.lang.module.ModuleDescriptor;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;

public class ModuleAttribute extends AttributeInfo {
    private int moduleNameIndex;
    private final int moduleFlags;
    private int moduleVersionIndex;
    private List<Requires> requires = new ArrayList<>();
    private List<Exports> exports = new ArrayList<>();
    private List<Opens> opens = new ArrayList<>();
    private List<Integer> usesIndices = new ArrayList<>();
    private List<Provides> provides = new ArrayList<>();

    public ModuleAttribute(int attributeNameIndex, int attributeLength, int moduleNameIndex, int moduleFlags,
                           int moduleVersionIndex) {
        super(attributeNameIndex, attributeLength);
        this.moduleNameIndex = moduleNameIndex;
        this.moduleFlags = moduleFlags;
        this.moduleVersionIndex = moduleVersionIndex;
    }


    public void addRequires(Requires requires) { this.requires.add(requires); }
    public void addExports(Exports exports) { this.exports.add(exports); }
    public void addOpens(Opens opens) { this.opens.add(opens); }
    public void addUses(int usesIndex) { this.usesIndices.add(usesIndex); }
    public void addProvides(Provides provides) { this.provides.add(provides); }

    public void setModuleNameIndex(int moduleNameIndex) { this.moduleNameIndex = moduleNameIndex; }
    public void setModuleVersionIndex(int newModuleVersionIndex) { moduleVersionIndex = newModuleVersionIndex; }

    public int moduleNameIndex() { return moduleNameIndex; }
    public int moduleFlags() { return moduleFlags; }
    public int moduleVersionIndex() { return moduleVersionIndex; }
    public List<Requires> requires() { return requires; }
    public List<Exports> exports() { return exports; }
    public List<Opens> opens() { return opens; }
    public List<Integer> usesIndices() { return usesIndices; }
    public List<Provides> provides() { return provides; }

    public EnumSet<ModuleDescriptor.Modifier> getModuleModifiers() {
        EnumSet<ModuleDescriptor.Modifier> modifiers = EnumSet.noneOf(ModuleDescriptor.Modifier.class);
        if ((moduleFlags & 0x0020) != 0) modifiers.add(ModuleDescriptor.Modifier.OPEN);
        if ((moduleFlags & 0x1000) != 0) modifiers.add(ModuleDescriptor.Modifier.SYNTHETIC);
        if ((moduleFlags & 0x8000) != 0) modifiers.add(ModuleDescriptor.Modifier.MANDATED);
        return modifiers;
    }

    public void removeRequiresIf(Predicate<Requires> removePredicate) {
        requires.removeIf(removePredicate);
    }

    @Override
    protected void attributeSpecificAppendTo(Output output) {
        output.appendU2(moduleNameIndex);
        output.appendU2(moduleFlags);
        output.appendU2(moduleVersionIndex);

        output.appendU2(requires.size());
        requires.forEach(r -> r.appendTo(output));

        output.appendU2(exports.size());
        exports.forEach(e -> e.appendTo(output));

        output.appendU2(opens.size());
        opens.forEach(o -> o.appendTo(output));

        output.appendU2(usesIndices.size());
        usesIndices.forEach(output::appendU2);

        output.appendU2(provides.size());
        provides.forEach(p -> p.appendTo(output));
    }
}
