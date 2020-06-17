package no.ion.jhms.modularizer;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

public class ConstantUtf8 extends ConstantPoolEntry {
    private final byte[] bytes;
    private final int start;
    private final int end;

    private String resolved = null;

    public static ConstantUtf8 fromString(String text) {
        var byteArray = new ByteArrayOutputStream(text.length() * 6 + 10);
        text.codePoints().forEach(codePoint -> {
            if (1 <= codePoint && codePoint <= 0x7F) {
                byteArray.write(codePoint);
            } else if (0 <= codePoint && codePoint <= 0x7FF) {
                byteArray.write(0b11000000 | (0b00111111 & (codePoint >>> 6)));
                byteArray.write(0b10000000 | (0b00111111 & codePoint));
            } else if (0x800 <= codePoint && codePoint <= 0xFFFF) {
                byteArray.write(0b11100000 | (0b00111111 & (codePoint >>> 12)));
                byteArray.write(0b10000000 | (0b00111111 & (codePoint >>> 6)));
                byteArray.write(0b10000000 | (0b00111111 & codePoint));
            } else {
                byteArray.write(0b11101101);
                byteArray.write(0b1010000 | (0b00001111 & ((codePoint >>> 16) - 1)));
                byteArray.write(0b10000000 | (0b00111111 & (codePoint >>> 10)));
                byteArray.write(0b11101101);
                byteArray.write(0b10110000 | (0b00001111 & (codePoint >>> 6)));
                byteArray.write(0b10000000 | (0b00111111 & codePoint));
            }
        });

        byte[] bytes = byteArray.toByteArray();
        return new ConstantUtf8(bytes, 0, bytes.length, text);
    }

    public ConstantUtf8(byte[] bytes, int start, int end) {
        this(bytes, start, end, null);
    }

    private ConstantUtf8(byte[] bytes, int start, int end, String resolved) {
        super(Constant.Utf8);
        this.bytes = bytes;
        this.start = start;
        this.end = end;
        this.resolved = resolved;
    }

    @Override
    public String toString() {
        if (resolved == null) {
            resolved = resolveToString();
        }
        return resolved;
    }

    @Override
    protected void entrySpecificAppendTo(Output output) {
        output.appendU2(end - start);
        output.writeByteArray(bytes, start, end - start);
    }

    private String resolveToString() {
        var builder = new StringBuilder(end - start);

        for (int index = start; index < end; ) {
            byte x = bytes[index++];
            final int codePoint;
            if (0 < x) {
                codePoint = x;
            } else {
                byte y = bytes[index++];
                if (matchPrefixBits(x, 0b110, 3) && matchPrefixBits(y, 0b10, 2)) {
                    codePoint = ((x & 0x1f) << 6) + (y & 0x3f);
                } else {
                    byte z = bytes[index++];
                    if (matchPrefixBits(x, 0b1110, 4) && matchPrefixBits(y, 0b10, 2) && matchPrefixBits(z, 0b10, 2)) {
                        codePoint = ((x & 0xf) << 12) + ((y & 0x3f) << 6) + (z & 0x3f);
                    } else {
                        byte u = x;
                        byte v = y;
                        byte w = z;
                        x = bytes[index++];
                        y = bytes[index++];
                        z = bytes[index++];

                        if ((u == (byte) 0b11101101) && matchPrefixBits(v, 0b1010, 4) && matchPrefixBits(w, 0b10, 2) &&
                            (x == (byte) 0b11101101) && matchPrefixBits(y, 0b1011, 4) && matchPrefixBits(z, 0b10, 2)) {
                            codePoint = 0x10000 + ((v & 0x0f) << 16) + ((w & 0x3f) << 10) + ((y & 0x0f) << 6) + (z & 0x3f);
                        } else {
                            throw new BadModuleInfoException("Failed to deserialize CONSTANT_Utf8 constant pool entry " +
                                    "at byte index " + index);
                        }
                    }
                }
            }

            builder.appendCodePoint(codePoint);
        }

        return builder.toString();
    }

    /** matchesBits(foo, 0b1001, 4) returns true iff the 4 highest bits of foo is 0b1001. */
    private boolean matchPrefixBits(byte value, int requiredValueOfHighestBits, int bitsInRequiredValueOfHighestBits) {
        return (value >>> (8 - bitsInRequiredValueOfHighestBits)) == requiredValueOfHighestBits;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConstantUtf8 that = (ConstantUtf8) o;
        return kind() == that.kind() && Arrays.equals(bytes, start, end, that.bytes, that.start, that.end);
    }

    @Override
    public int hashCode() {
        int result = 1;
        for (int i = start; i < end; ++i) {
            result = 31 * result + bytes[i];
        }
        return result;
    }
}
