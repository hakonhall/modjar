package no.ion.jhms.modularizer;

import java.util.ArrayList;
import java.util.List;

public class AttributeInfos {
    private final List<AttributeInfo> attributes = new ArrayList<>();
    private ModuleAttribute moduleAttribute = null;
    private SourceFileAttribute sourceFileAttribute = null;

    public void add(AttributeInfo attribute) {
        attributes.add(attribute);
    }

    public void addModuleAttribute(ModuleAttribute moduleAttribute) {
        if (this.moduleAttribute != null) {
            throw new BadModuleInfoException("More than one Module attribute");
        }

        this.moduleAttribute = moduleAttribute;
        add(moduleAttribute);
    }

    public void addSourceFile(SourceFileAttribute sourceFileAttribute) {
        if (this.sourceFileAttribute != null) {
            throw new BadModuleInfoException("More than one SourceFile attribute");
        }

        this.sourceFileAttribute = sourceFileAttribute;
        add(sourceFileAttribute);
    }

    public ModuleAttribute getModuleAttribute() {
        return moduleAttribute;
    }

    public void validate() {
        if (moduleAttribute == null) {
            throw new BadModuleInfoException("Missing Module attribute");
        }
    }

    public void appendTo(Output output) {
        output.appendU2(attributes.size());
        for (var attribute : attributes) {
            attribute.appendTo(output);
        }
    }
}
