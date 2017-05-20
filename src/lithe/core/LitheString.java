package lithe.core;

import java.io.*;
import java.nio.charset.Charset;
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

        try (PushbackInputStream bais = new PushbackInputStream(new ByteArrayInputStream(input.getBytes(Charset.forName("UTF-8"))))) {
            boolean caps = false;
            byte byt;
            while ((byt = (byte) bais.read()) != -1) {
                if (byt >= 97 && byt <= 122) { // lower
                    if (caps) {
                        output.write01("11011");
                        caps = !caps;
                    }
                    output.writeLast5Bits((byte) (byt - 97));

                } else if (byt >= 65 && byt <= 90) { // upper
                    if (!caps) {
                        output.write01("11011");
                        caps = !caps;
                    }
                    output.writeLast5Bits((byte) (byt - 65));

                } else if (byt == 32) { //space
                    output.write01("11010");
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

    private static byte[] z3(String input) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write(0b10111111);
        try {
            try (Writer writer = new OutputStreamWriter(new GZIPOutputStream(output), Charset.forName("UTF-8"))) {
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
        //TODO
        return null;
    }

    public static byte[] shortest(byte[]... bytes) {
        byte[] shortest = bytes[0];
        for (int i = 1; i < bytes.length; i++) {
            if (bytes[i].length < shortest.length) {
                shortest = bytes[i];
            }
        }
        return shortest;
    }
/*

    private static class BitWriter {
        private final BitSet b;
        private int counter = 0;

        public BitWriter(BitSet b) {
            this.b = b;
        }

        public void write(boolean bit) {
            b.set(counter++, bit);
        }

        public void write01(String binaryString) {
            char[] chars = binaryString.toCharArray();
            for (char c : chars) {
                b.set(counter++, c == '1');
            }
        }


        public void writeChar(char c) {
            byte[] bytes = new String(new char[]{c}).getBytes(Charset.forName("UTF-8"));
            write(bytes);
        }

        public void write(byte value) {
            for (int bit = 0; bit < 7; bit++) {
                b.set(counter++, (value & (1 << bit)) != 0);
            }
        }

        public void write(byte[] values) {
            for (byte value : values) {
                write(value);
            }
        }

        public byte[] toByteArray() {
            return b.toByteArray();
        }


    }
    */


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

/*
        public void writeChar(char c) {
            byte[] bytes = new String(new char[]{c}).getBytes(Charset.forName("UTF-8"));
            write(bytes);
        }*/

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

        public void write(byte[] values) {
            for (byte value : values) {
                write(value);
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

    //TODO
    private static class BitReader {
        private static final int START = 7;
        private final ByteArrayOutputStream b = new ByteArrayOutputStream();
        private boolean closed = false;
        private byte current = 0;
        private int curpos = START;
        private ByteArrayInputStream bais;

        public BitReader(byte[] b) {
            bais = new ByteArrayInputStream(b);
            privateRead();
        }

        private void privateRead() {
            int read = bais.read();
            if (read == -1) {
                closed = true;
                current = 0;
            } else {
                current = (byte) read;
            }
        }

        public byte read() {
            if (curpos == START) {
                byte toRet = current;
                privateRead();
                return toRet;
            } else {
                for (int bit = 8; bit-- > 0; ) {

                }

            }


            return 0;

        }

        public boolean isClosed() {
            return closed;
        }

        public boolean peek01(String s) {
            return false;
        }
    }


    private static boolean getNBitValue(byte value, byte n) {
        return (value & (1 << n)) != 0;
    }


}
