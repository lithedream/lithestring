package io.github.lithedream.lithestring.internal;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

class UTF8Char {
    private static UTF8Char invalidChar = null;
    private final byte[] bytes;

    public UTF8Char(byte[] bytes) {
        this.bytes = bytes;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof UTF8Char)) {
            return false;
        }
        return Arrays.equals(this.bytes, ((UTF8Char) obj).bytes);
    }

    public byte[] getBytes() {
        return bytes;
    }

    @Override
    public int hashCode() {
        if (bytes.length == 1) {
            return bytes[0];
        } else if (bytes.length == 2) {
            return ((0xFF & bytes[1]) << 8) | (0xFF & bytes[0]);
        } else if (bytes.length == 3) {
            return ((0xFF & bytes[2]) << 16) | ((0xFF & bytes[1]) << 8) | (0xFF & bytes[0]);
        } else {
            return ((0xFF & bytes[3]) << 24) | ((0xFF & bytes[2]) << 16) | ((0xFF & bytes[1]) << 8) | (0xFF & bytes[0]);
        }
    }

    public String asString() {
        return new String(bytes, StandardCharsets.UTF_8);
    }

    @Override
    public String toString() {
        return "UTF8Char:[" + asString() + "][" + as01String() + "]";
    }

    public String as01String() {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            for (int bit = 8; bit-- > 0;) {
                sb.append(((b & (1 << bit)) != 0) ? '1' : '0');
            }
            sb.append(' ');
        }
        return sb.toString();
    }

    public static UTF8Char getInvalidChar() {
        if (invalidChar == null) {
            invalidChar = new UTF8Char(new byte[] { (byte) 0b10000000 });
        }
        return invalidChar;
    }

    public boolean isInvalid() {
        return bytes.length == 1 && bytes[0] == (byte) 0b10000000;
    }
}