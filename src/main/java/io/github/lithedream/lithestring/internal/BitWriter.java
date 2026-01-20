package io.github.lithedream.lithestring.internal;

import java.io.ByteArrayOutputStream;

class BitWriter {
    private static final int START = 7;
    private final ByteArrayOutputStream b = new ByteArrayOutputStream();
    private byte current = 0;
    private int curpos = START;

    private boolean closed = false;

    public BitWriter() {
    }

    public void write(boolean bit) {
        if (bit) {
            current |= 1 << curpos;
        } else {
            current &= ~(1 << curpos);
        }
        if (curpos == 0) {
            b.write(current);
            current = 0;
            curpos = START;
        } else {
            curpos--;
        }
    }

    public void write01(String binaryString) {
        char[] chars = binaryString.toCharArray();
        for (char c : chars) {
            write(c == '1');
        }
    }

    public void write(byte value) {
        for (byte bit = 8; bit-- > 0;) {
            write(Utils.getNBitValue(value, bit));
        }
    }

    public void writeLast5Bits(byte value) {
        for (byte bit = 5; bit-- > 0;) {
            write(Utils.getNBitValue(value, bit));
        }
    }

    public void close() {
        if (!closed) {
            closed = true;
            if (curpos != START) {
                b.write(current);
            }
        }
    }

    public int getSpareBits() {
        if (curpos == START) {
            return 0;
        }
        return curpos + 1;
    }

    public byte[] toByteArray() {
        if (closed || curpos == START) {
            return b.toByteArray();
        } else {
            byte[] bytes = b.toByteArray();
            byte[] bytes2 = new byte[bytes.length + 1];
            System.arraycopy(bytes, 0, bytes2, 0, bytes.length);
            bytes2[bytes.length] = current;
            return bytes2;
        }
    }

}