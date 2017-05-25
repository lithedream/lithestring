package lithe.core;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class LitheString {
    private byte[] content;

    public LitheString(String input) {
        this.content = zip(input);
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || (obj != null && (obj instanceof LitheString) && ((LitheString) obj).content.equals(this.content));
    }

    public byte[] getBytes() {
        return content;
    }

    public String getString() {
        return unzip(content);
    }


    public static byte[] zip(String input) {

        byte[] z0 = input.getBytes(Charset.forName("UTF-8"));
        byte[] z1 = z1(input);
        //byte[] z2 = z2(input);
        byte[] z3 = z3(input);

        return shortest(z0, z1, /*z2,*/ z3);
    }

    public static byte[] z1(String input) {
        BitWriter output = new BitWriter();
        output.write01("100");

        try (PushbackInputStream bais = new PushbackInputStream(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)))) {
            boolean caps = false;
            byte byt;
            while ((byt = (byte) bais.read()) != -1) {
                if (byt >= 97 && byt <= 122) { // lower
                    if (caps) {
                        output.write01("00000");
                        caps = !caps;
                    }
                    output.writeLast5Bits((byte) (byt - 96));

                } else if (byt >= 65 && byt <= 90) { // upper
                    if (!caps) {
                        output.write01("00000");
                        caps = !caps;
                    }
                    output.writeLast5Bits((byte) (byt - 64));

                } else if (byt == 32) { //space
                    output.write01("11011");
                } else {

                    int nExtraByte = 0;
                    if (!startsWith(byt, "0")) { //1 byte
                        if (startsWith(byt, "110")) { //2 byte
                            nExtraByte = 1;
                        } else if (startsWith(byt, "1110")) { //3 byte
                            nExtraByte = 2;
                        } else if (startsWith(byt, "11110")) { //4 byte
                            nExtraByte = 3;
                        }
                    }

                    output.write01("111");
                    output.write(byt);
                    for (int i = 0; i < nExtraByte; i++) {
                        output.write((byte) bais.read());
                    }
                }
            }
        } catch (IOException e) {

        }
        return output.toByteArray();
    }

    private static boolean startsWith(byte byt, String binaryString) {
        byte pos = 7;
        for (int i = 0; i < binaryString.length(); i++) {
            boolean iBitIsSet = binaryString.charAt(i) == ('1');
            boolean bitValue = getNBitValue(byt, pos);
            if (bitValue != iBitIsSet) {
                return false;
            }
            if (pos == 0) {
                break;
            } else {
                pos--;
            }
        }
        return true;
    }

    private static byte[] z2(String input) {

        BitWriter output = new BitWriter();
        output.write01("1010");

        output.close();
        return output.toByteArray();

    }

    public static byte[] z3(String input) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write(0b10111111);
        try {
            try (Writer writer = new OutputStreamWriter(new GZIPOutputStream(output), StandardCharsets.UTF_8)) {
                writer.write(input);
            }
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        } finally {
            try {
                output.close();
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
        }

        return output.toByteArray();
    }

    public static String unzip(byte[] content) {
        if (content == null) {
            return null;
        }
        if (content.length == 0) {
            return "";
        }
        if (startsWith(content[0], "100")) {
            return unzip1(content);
        }
        if ((content[0] & 0xFF) == 0b10111111) {
            return unzip3(content);
        }
        return new String(content, StandardCharsets.UTF_8);
    }

    private static String unzip3(byte[] content) {
        ByteArrayInputStream bais = new ByteArrayInputStream(content);
        bais.read();
        try (GZIPInputStream gis = new GZIPInputStream(bais)) {
            byte[] buffer = new byte[1024];
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            int len;
            while ((len = gis.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }

            gis.close();
            out.close();
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }

    }

    private static String unzip1(byte[] content) {
        BitReader bitReader = new BitReader(content);
        bitReader.advance(3);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        boolean caps = false;
        while (!bitReader.isClosed()) {
            if (bitReader.peek01("111")) {
                bitReader.advance(3);
                byte read = bitReader.read();

                int nExtraByte = 0;
                if (!startsWith(read, "0")) { //1 byte
                    if (startsWith(read, "110")) { //2 byte
                        nExtraByte = 1;
                    } else if (startsWith(read, "1110")) { //3 byte
                        nExtraByte = 2;
                    } else if (startsWith(read, "11110")) { //4 byte
                        nExtraByte = 3;
                    }
                }
                baos.write(read);
                for (int i = 0; i < nExtraByte; i++) {
                    baos.write(bitReader.read());
                }
            } else {
                byte read = bitReader.read(5);
                if (read == 0) {
                    caps = !caps;
                } else if ((read & 0xFF) == 0b00011011) {
                    baos.write(32); // space
                } else {
                    baos.write(read + (caps ? 64 : 96)); // UPPER:lower
                }
            }
        }
        return new String(baos.toByteArray(), StandardCharsets.UTF_8);
    }

    private static byte[] shortest(byte[]... bytes) {
        byte[] shortest = bytes[0];
        for (int i = 1; i < bytes.length; i++) {
            if (bytes[i].length < shortest.length) {
                shortest = bytes[i];
            }
        }
        return shortest;
    }

    private static class BitWriter {
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
            for (byte bit = 8; bit-- > 0; ) {
                write(getNBitValue(value, bit));
            }
        }

        public void writeLast5Bits(byte value) {
            for (byte bit = 5; bit-- > 0; ) {
                write(getNBitValue(value, bit));
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

    private static class BitReader {
        private static final byte START = 7;
        private byte curpos = START;
        private byte[] bytes;
        private int bytePos = 0;

        public BitReader(byte[] b) {
            bytes = b;
        }

        public byte read() {
            if (curpos == START) {
                if (bytePos >= bytes.length) {
                    return 0; //force return 0 because the stream ends
                }
                byte toRet = bytes[bytePos];
                bytePos++;
                return toRet;
            } else {
                byte toRet = 0;
                for (int bit = 8; bit-- > 0; ) {
                    if (bytePos >= bytes.length) {
                        return 0; //force return 0 because the stream ends
                    }
                    if (getNBitValue(bytes[bytePos], curpos)) {
                        toRet |= 1 << bit;
                    } else {
                        toRet &= ~(1 << bit);
                    }
                    if (curpos == 0) {
                        bytePos++;
                        curpos = START;
                    } else {
                        curpos--;
                    }
                }
                return toRet;

            }
        }

        public byte read(int numberOfBits) {
            byte toRet = 0;
            for (int bit = numberOfBits; bit-- > 0; ) {
                if (bytePos >= bytes.length) {
                    return 0; //force return 0 because the stream ends
                }
                if (getNBitValue(bytes[bytePos], curpos)) {
                    toRet |= 1 << bit;
                } else {
                    toRet &= ~(1 << bit);
                }
                if (curpos == 0) {
                    bytePos++;
                    curpos = START;
                } else {
                    curpos--;
                }
            }
            return toRet;
        }

        public boolean isClosed() {
            return bytePos >= bytes.length;
        }

        public void advance(int n) {
            for (int i = 0; i < n; i++) {
                if (curpos == 0) {
                    bytePos++;
                    curpos = START;
                } else {
                    curpos--;
                }
            }
        }

        public boolean peek01(String s) {
            int bytePosCopy = bytePos;
            byte curPosCopy = curpos;

            for (int i = 0; i < s.length(); i++) {
                if (bytePosCopy >= bytes.length) {
                    return false;
                }
                boolean bitValue = getNBitValue(bytes[bytePosCopy], curPosCopy);
                boolean iBitIsSet = s.charAt(i) == ('1');
                if (bitValue != iBitIsSet) {
                    return false;
                }

                if (curPosCopy == 0) {
                    bytePosCopy++;
                    curPosCopy = START;
                } else {
                    curPosCopy--;
                }
            }
            return true;
        }
    }


    private static boolean getNBitValue(byte value, byte n) {
        return (value & (1 << n)) != 0;
    }


}
