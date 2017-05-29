package lithe.core;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
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
            int in;
            while ((in = bais.read()) != -1) {
                byte byt = (byte) in;
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
                    output.write01("111");
                    output.write(byt);
                    for (int i = 0; i < getNExtraBytes(byt); i++) {
                        output.write((byte) bais.read());
                    }
                }
            }
        } catch (IOException e) {

        }
        output.close();
        return output.toByteArray();
    }

    private static int getNExtraBytes(byte byt) {
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
        return nExtraByte;
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

    public static byte[] z2(String input) {
        List<UTF8Char> listChars = new ArrayList<>();
        try (ByteArrayInputStream bais = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8))) {
            int in;
            while ((in = bais.read()) != -1) {
                byte byt = (byte) in;
                int nExtraByte = getNExtraBytes(byt);

                byte[] utf8Bytes = new byte[nExtraByte + 1];
                utf8Bytes[0] = byt;

                for (int i = 0; i < nExtraByte; i++) {
                    utf8Bytes[i + 1] = (byte) bais.read();
                }
                listChars.add(new UTF8Char(utf8Bytes));
            }
        } catch (IOException e) {

        }

        BitWriter output = innerZ2(listChars, 0);
        int spareBits = output.getSpareBits();
        if (spareBits > 0) {
            output = innerZ2(listChars, spareBits);
        }
        output.close();
        return output.toByteArray();
    }

    private static BitWriter innerZ2(List<UTF8Char> listChars, int spareBits) {
        BitWriter output = new BitWriter();
        output.write01("1010");
        for (int i = 0; i < spareBits; i++) {
            output.write01("0");
        }
        output.write01("1");
        Map<UTF8Char, String> huff = Huffer.huff(listChars);
        TreeMap<String, UTF8Char> reverse = new TreeMap<>();
        for (Map.Entry<UTF8Char, String> e : huff.entrySet()) {
            reverse.put(e.getValue(), e.getKey());
        }
        int length = 0;
        for (Map.Entry<String, UTF8Char> e : reverse.entrySet()) {
            int difference = e.getKey().length() - length;
            if (difference > 0) {
                for (int i = 0; i < difference; i++) {
                    output.write01("0");
                }
                output.write01("1");
                length = e.getKey().length();
            } else {
                output.write01("10");
            }
            output.write01(e.getKey());

            byte byt = e.getValue().getFirst();
            if (byt >= 97 && byt <= 122) { // lower
                output.writeLast5Bits((byte) (byt - 96));

            } else if (byt == 32) { //space
                output.write01("11011");
            } else {
                output.write01("111");
                for (int i = 0; i < e.getValue().getBytes().length; i++) {
                    output.write(e.getValue().getBytes()[i]);

                }
            }
        }
        output.write01("11");
        for (UTF8Char c : listChars) {
            output.write01(huff.get(c));
        }
        return output;
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

                baos.write(read);
                for (int i = 0; i < getNExtraBytes(read); i++) {
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

    private static class Huffer {

        private static class HuffmanTree<T> implements Comparable<HuffmanTree<T>> {

            public final int freq;

            public final HuffmanTree<T> l, r;

            public final T value;

            private final boolean isLeaf;

            public HuffmanTree(HuffmanTree<T> l, HuffmanTree<T> r) {
                freq = l.freq + r.freq;
                this.l = l;
                this.r = r;
                value = null;
                isLeaf = false;
            }

            public HuffmanTree(int freq, T value) {
                this.freq = freq;
                this.value = value;
                l = null;
                r = null;
                isLeaf = true;
            }

            public int compareTo(HuffmanTree o) {
                return freq - o.freq;
            }

        }


        private static <T> void toMap(HuffmanTree<T> tree, StringBuilder prefix, Map<T, String> t) {
            if (tree.isLeaf) {
                t.put(tree.value, prefix.toString());
            } else {
                prefix.append('0');
                toMap(tree.l, prefix, t);
                prefix.deleteCharAt(prefix.length() - 1);

                prefix.append('1');
                toMap(tree.r, prefix, t);
                prefix.deleteCharAt(prefix.length() - 1);
            }
        }

        private static <T> HuffmanTree makeHuffmanTree(Map<T, Integer> objFreqs) {
            PriorityQueue<HuffmanTree<T>> huffmanTrees = new PriorityQueue<HuffmanTree<T>>();
            for (Map.Entry<T, Integer> entry : objFreqs.entrySet()) {
                huffmanTrees.offer(new HuffmanTree<T>(entry.getValue(), entry.getKey()));
            }
            while (huffmanTrees.size() > 1) {
                HuffmanTree<T> l = huffmanTrees.poll();
                HuffmanTree<T> r = huffmanTrees.poll();
                huffmanTrees.offer(new HuffmanTree<T>(l, r));
            }
            return huffmanTrees.poll();
        }

        public static <T> Map<T, String> huff(Collection<T> input) {
            Map<T, Integer> objectFreqs = new HashMap<>();
            for (T obj : input) {
                Integer count = objectFreqs.get(obj);
                objectFreqs.put(obj, count == null ? 1 : count + 1);
            }
            Map<T, String> map = new HashMap<T, String>();
            if (objectFreqs.size() == 1) {
                map.put(objectFreqs.entrySet().iterator().next().getKey(), "1");
            } else {
                toMap(makeHuffmanTree(objectFreqs), new StringBuilder(), map);
            }
            return map;
        }

    }

    private static class UTF8Char {
        private final byte[] bytes;

        private UTF8Char(byte[] bytes) {
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


        public byte getFirst() {
            return bytes[0];
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

    }
}