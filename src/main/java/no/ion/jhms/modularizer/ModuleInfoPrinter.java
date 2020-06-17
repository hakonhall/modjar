package no.ion.jhms.modularizer;

import java.lang.module.ModuleDescriptor;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Can be used to write a {@link ModuleInfoClass} as a String compatible with module-info.java, without annotations.
 */
public class ModuleInfoPrinter {
    private final ModuleDescriptor descriptor;
    private final StringBuilder builder = new StringBuilder();

    private int lineno = 1;
    private boolean beginningOfLine = true;
    private int indentationLevel = 0;
    private final String indent = "    ";

    public ModuleInfoPrinter(ModuleDescriptor moduleDescriptor) {
        this.descriptor = moduleDescriptor;
    }

    /** Get multi-line String that can be used as module-info.java (unless annotations have been used). */
    public String getModuleInfoJava() {
        return toString();
    }

    @Override
    public String toString() {
        if (builder.length() == 0) {

            if (descriptor.isOpen()) {
                append("open ");
            }

            append("module ").append(descriptor.name()).append(" {");

            var suffix = new StringBuilder();
            descriptor.version().ifPresent(version -> suffix.append(" @").append(version.toString()));
            if (descriptor.modifiers().contains(ModuleDescriptor.Modifier.AUTOMATIC)) {
                // Actually not possible since it cannot be specified in class file.
                suffix.append(" automatic");
            }
            if (descriptor.modifiers().contains(ModuleDescriptor.Modifier.SYNTHETIC)) {
                suffix.append(" synthetic");
            }
            if (descriptor.modifiers().contains(ModuleDescriptor.Modifier.MANDATED)) {
                suffix.append(" mandated");
            }

            if (suffix.length() == 0) {
                appendLine();
            } else {
                append(" //").appendLine(suffix.toString());
            }

            {
                ++indentationLevel;

                appendLine();

                int previousLineno = lineno;

                // Unfortunately the order in the module descriptor is lost by ModuleDescriptor.Requires.
                descriptor.requires().stream()
                        .sorted(Comparator.comparing(ModuleDescriptor.Requires::name))
                        .forEach(this::appendRequires);

                if (previousLineno < lineno) {
                    appendLine();
                    previousLineno = lineno;
                }

                descriptor.exports().stream()
                        .sorted(Comparator.comparing(ModuleDescriptor.Exports::source))
                        .forEach(this::appendExports);

                if (previousLineno < lineno) {
                    appendLine();
                    previousLineno = lineno;
                }

                descriptor.opens().stream()
                        .sorted(Comparator.comparing(ModuleDescriptor.Opens::source))
                        .forEach(this::appendOpens);

                if (previousLineno < lineno) {
                    appendLine();
                    previousLineno = lineno;
                }

                descriptor.uses().stream().sorted().forEach(use -> append("use ").append(use).appendLine(";"));

                if (previousLineno < lineno) {
                    appendLine();
                    previousLineno = lineno;
                }

                descriptor.provides().stream()
                        .sorted(Comparator.comparing(ModuleDescriptor.Provides::service))
                        .forEach(this::appendProvides);

                if (previousLineno < lineno) {
                    appendLine();
                    previousLineno = lineno;
                }

                --indentationLevel;
            }
            appendFormatLine("}");
        }

        return builder.toString();
    }

    private void appendProvides(ModuleDescriptor.Provides provides) {
        append("provides ").append(provides.service());

        List<String> providers = provides.providers();
        if (!providers.isEmpty()) {
            appendLine(" with");
            ++indentationLevel;

            boolean firstLine = true;
            for (var provider : providers.stream().sorted().collect(Collectors.toList())) {
                if (firstLine) {
                    firstLine = false;
                    append(provider);
                } else {
                    appendLine(",");
                }
            }

            --indentationLevel;
        }
        appendLine(";");
    }

    private void appendOpens(ModuleDescriptor.Opens opens) {
        append("opens ");
        append(opens.source());

        if (opens.isQualified()) {
            appendLine(" to");
            ++indentationLevel;

            boolean firstLine = true;
            for (var target : opens.targets().stream().sorted().collect(Collectors.toList())) {
                if (firstLine) {
                    firstLine = false;
                } else {
                    appendLine(",");
                }
                append(target);
            }

            --indentationLevel;
        }
        append(";");

        var suffix = new StringBuilder();
        if (opens.modifiers().contains(ModuleDescriptor.Opens.Modifier.SYNTHETIC)) {
            suffix.append(" synthetic");
        }
        if (opens.modifiers().contains(ModuleDescriptor.Opens.Modifier.MANDATED)) {
            suffix.append(" mandated");
        }

        if (suffix.length() == 0) {
            appendLine();
        } else {
            append(" //").appendLine(suffix.toString());
        }
    }

    private void appendExports(ModuleDescriptor.Exports exports) {
        append("exports ");
        append(exports.source());

        if (exports.isQualified()) {
            appendLine(" to");
            ++indentationLevel;

            boolean firstLine = true;
            for (var target : exports.targets().stream().sorted().collect(Collectors.toList())) {
                if (firstLine) {
                    firstLine = false;
                } else {
                    appendLine(",");
                }
                append(target);
            }

            --indentationLevel;
        }
        append(";");

        var suffix = new StringBuilder();
        if (exports.modifiers().contains(ModuleDescriptor.Exports.Modifier.SYNTHETIC)) {
            suffix.append(" synthetic");
        }
        if (exports.modifiers().contains(ModuleDescriptor.Exports.Modifier.MANDATED)) {
            suffix.append(" mandated");
        }

        if (suffix.length() == 0) {
            appendLine();
        } else {
            append(" //").appendLine(suffix.toString());
        }
    }

    private void appendRequires(ModuleDescriptor.Requires requires) {
        append("requires ");

        if (requires.modifiers().contains(ModuleDescriptor.Requires.Modifier.TRANSITIVE)) {
            append("transitive ");
        }

        if (requires.modifiers().contains(ModuleDescriptor.Requires.Modifier.STATIC)) {
            append("static ");
        }

        append(requires.name());
        append(";");

        var suffix = new StringBuilder();

        requires.compiledVersion().ifPresent(version -> suffix.append(" @").append(version.toString()));

        if (requires.modifiers().contains(ModuleDescriptor.Requires.Modifier.SYNTHETIC)) {
            suffix.append(" synthetic");
        }

        if (requires.modifiers().contains(ModuleDescriptor.Requires.Modifier.MANDATED)) {
            suffix.append(" mandated");
        }

        if (suffix.length() == 0) {
            appendLine();
        } else {
            append(" //");
            appendLine(suffix.toString());
        }
    }

    private ModuleInfoPrinter appendFormatLine(String format, Object... args) {
        append(String.format(format, args));
        return appendLine();
    }

    private ModuleInfoPrinter appendLine() { return append("\n"); }

    private ModuleInfoPrinter appendLine(String line) {
        append(line);
        return appendLine();
    }

    private ModuleInfoPrinter append(String text) {
        if (beginningOfLine) {
            ++lineno;  // "", "foo", and "foo\n" are one line. "foo\nbar" and "foo\nbar\n" are two.

            if (text.equals("\n")) {
                // avoid indentation
                builder.append('\n');
                return this;
            }

            beginningOfLine = false;

            append(indent.repeat(indentationLevel));
        }

        builder.append(text);

        beginningOfLine = text.endsWith("\n");

        return this;
    }
}
