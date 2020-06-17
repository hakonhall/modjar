package no.ion.jhms.modularizer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import static java.lang.Integer.min;
import static no.ion.jhms.modularizer.Exceptions.uncheckIO;

public class Output {
    private static final int DEFAULT_BYTE_ARRAY_SIZE = 8192;

    private final int byteArraySize;
    private final ArrayList<byte[]> byteArrays = new ArrayList<>();

    /** The end byte in the last byte array of byteArrays. */
    private int endIndex;

    public Output() { this(DEFAULT_BYTE_ARRAY_SIZE); }

    Output(int byteArraySize) {
        this.byteArraySize = byteArraySize;
        addByteArray();
    }

    public int size() {
        return byteArraySize * (byteArrays.size() - 1) + endIndex;
    }

    public void appendU1(int unsignedByte) {
        if (endIndex >= byteArraySize) {
            addByteArray();
        }

        byte[] lastByteArray = lastByteArray();
        lastByteArray[endIndex] = (byte) unsignedByte;
        ++endIndex;
    }

    public void appendU2(int unsignedShort) {
        appendU1((unsignedShort >>> 8) & 0xFF);
        appendU1((unsignedShort >>> 0) & 0xFF);
    }

    public void appendU4(int unsignedInt) {
        appendU1((unsignedInt >>> 24) & 0xFF);
        appendU1((unsignedInt >>> 16) & 0xFF);
        appendU1((unsignedInt >>> 8) & 0xFF);
        appendU1((unsignedInt >>> 0) & 0xFF);
    }

    public void writeByteArray(byte[] bytes, int start, int length) {
        int index = start;
        int end = start + length;
        while (index < end) {
            if (endIndex == byteArraySize) {
                addByteArray();
            }

            byte[] lastByteArray = lastByteArray();
            int copyLength = min(byteArraySize - endIndex, end - index);

            System.arraycopy(bytes, index, lastByteArray, endIndex, copyLength);
            index += copyLength;
            endIndex += copyLength;
        }
    }

    /** The byte written at offset is the same as appending when size() == offset.  */
    public void writeU1At(int offset, int unsignedByte) {
        int size = size();
        if (offset < 0 || offset > size) {
            throw new ArrayIndexOutOfBoundsException(offset);
        } else if (offset == size) {
            appendU1(unsignedByte);
        } else {
            byte[] byteArray = byteArrays.get(offset / byteArraySize);
            byteArray[offset % byteArraySize] = (byte) unsignedByte;
        }
    }

    /** The bytes written at offset is the same as appending when size() == offset.  */
    public void writeU2At(int offset, int unsignedShort) {
        writeU1At(offset + 0, (unsignedShort >>> 8) & 0xFF);
        writeU1At(offset + 1, (unsignedShort >>> 0) & 0xFF);
    }

    /** The bytes written at offset is the same as appending when size() == offset.  */
    public void writeU4At(int offset, int unsignedInt) {
        writeU1At(offset + 0, (unsignedInt >>> 24) & 0xFF);
        writeU1At(offset + 1, (unsignedInt >>> 16) & 0xFF);
        writeU1At(offset + 2, (unsignedInt >>> 8) & 0xFF);
        writeU1At(offset + 3, (unsignedInt >>> 0) & 0xFF);
    }

    public void writeTo(Path path) {
        try (var outputStream = Files.newOutputStream(path)) {
            int i = 0;
            for (; i < byteArrays.size() - 1; ++i) {
                byte[] bytes = byteArrays.get(i);
                uncheckIO(() -> outputStream.write(bytes));
            }

            byte[] lastBytes = byteArrays.get(i);
            uncheckIO(() -> outputStream.write(lastBytes, 0, endIndex));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void writeTo(OutputStream outputStream) {
        int i = 0;
        for (; i < byteArrays.size() - 1; ++i) {
            byte[] bytes = byteArrays.get(i);
            uncheckIO(() -> outputStream.write(bytes));
        }

        byte[] lastBytes = byteArrays.get(i);
        uncheckIO(() -> outputStream.write(lastBytes, 0, endIndex));
    }

    public byte[] toByteArray() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(size());
        writeTo(outputStream);
        return outputStream.toByteArray();
    }

    private void addByteArray() {
        byteArrays.add(new byte[byteArraySize]);
        endIndex = 0;
    }

    private byte[] lastByteArray() {
        return byteArrays.get(byteArrays.size() - 1);
    }
}
