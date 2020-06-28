package no.ion.jhms.modularizer;

import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

import static java.lang.Integer.min;

public class UnicodePointer implements Iterable<Integer> {
    private static final Set<String> ILLEGAL_IDENTIFIERS = Set.of(
            // Keywords
            "abstract",     "continue",      "for",         "new",         "switch",
            "assert",       "default",       "if",          "package",     "synchronized",
            "boolean",      "do",            "goto",        "private",     "this",
            "break",        "double",        "implements",  "protected",   "throw",
            "byte",         "else",          "import",      "public",      "throws",
            "case",         "enum",          "instanceof",  "return",      "transient",
            "catch",        "extends",       "int",         "short",       "try",
            "char",         "final",         "interface",   "static",      "void",
            "class",        "finally",       "long",        "strictfp",    "volatile",
            "const",        "float",         "native",      "super",       "while",
            "_",

            // boolean literals
            "true", "false",

            // null literal
            "null");

    private final int[] codePoints;
    private int index;
    private final int endIndex;

    public UnicodePointer(String string) { this(string.codePoints().toArray()); }

    private UnicodePointer(int[] codePoints) { this(codePoints, 0, codePoints.length); }

    private UnicodePointer(int[] codePoints, int index, int endIndex) {
        this.codePoints = codePoints;
        this.index = index;
        this.endIndex = endIndex;
    }

    public UnicodePointer clone() { return new UnicodePointer(codePoints, index, endIndex); }

    public int size() { return codePoints.length - index; }
    public boolean eof() { return index >= codePoints.length; }
    public int get() { return get(0); }
    public int get(int index) { return codePoints[this.index + index]; }

    /** Return eof(). */
    public boolean inc() {
        if (eof()) return true;
        ++index;
        return eof();
    }

    public boolean skip(String prefix) {
        int startIndex = index;

        int[] prefixCodePoints = prefix.codePoints().toArray();
        for (int i = 0; i < prefixCodePoints.length; ++i, ++index) {
            if (eof() || get() != prefixCodePoints[i]) {
                index = startIndex;
                return false;
            }
        }

        return true;
    }

    /** Skip all {@link Character#isWhitespace(int)}, and return false if at least one were skipped. */
    public boolean skipWhitespace() {
        int startIndex = index;

        while (!eof() && Character.isWhitespace(get())) {
            ++index;
        }

        return index != startIndex;
    }

    public Optional<String> skipIdentifier() {
        if (eof() || !Character.isJavaIdentifierStart(get())) {
            return Optional.empty();
        }

        int startIndex = index++;

        while (!eof() && Character.isJavaIdentifierPart(get())) {
            ++index;
        }

        String identifier = new String(codePoints, startIndex, index - startIndex);
        if (ILLEGAL_IDENTIFIERS.contains(identifier)) {
            index = startIndex;
            return Optional.empty();
        }

        return Optional.of(identifier);
    }

    public Optional<String> skipModuleName() {
        try {
            return skipPackageOrModuleName();
        } catch (IllegalArgumentException e) {
            throw new ErrorException("not a module name: " + toString());
        }
    }

    public Optional<String> skipPackageName() {
        try {
            return skipPackageOrModuleName();
        } catch (IllegalArgumentException e) {
            throw new ErrorException("not a package name: " + toString());
        }
    }

    private Optional<String> skipPackageOrModuleName() {
        int startIndex = index;

        Optional<String> firstIdentifier = skipIdentifier();
        if (firstIdentifier.isEmpty()) {
            return Optional.empty();
        }

        var compactPackageName = new StringBuilder();
        compactPackageName.append(firstIdentifier.get());

        do {
            int endIndex = index;

            skipWhitespace();
            if (eof() || get() != '.') {
                index = endIndex;
                return Optional.of(compactPackageName.toString());
            }
            inc();
            compactPackageName.append('.');

            skipWhitespace();

            Optional<String> identifier = skipIdentifier();
            if (identifier.isEmpty()) {
                index = startIndex;
                throw new ErrorException("not a package name: " + toString());
            }

            compactPackageName.append(identifier.get());
        } while (true);
    }

    public UnicodePointer unicodeSubstring(int start, int length) {
        return new UnicodePointer(codePoints, index + start, index + start + length);
    }

    public String substring(int start, int length) {
        return new String(codePoints, index + start, index + start + length);
    }

    @Override
    public Iterator<Integer> iterator() {
        return new Iterator<>() {
            private int index = -1;

            @Override
            public boolean hasNext() { return index + 1 < codePoints.length; }

            @Override
            public Integer next() { return codePoints[++index]; }
        };
    };

    @Override public String toString() { return new String(codePoints, index, endIndex - index); }
}
