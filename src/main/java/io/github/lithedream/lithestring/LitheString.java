package io.github.lithedream.lithestring;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PushbackInputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;


public class LitheString {

    private byte[] compressed;

    protected LitheString(String input) {
        this.compressed = zip(input);
    }

    protected LitheString(byte[] compressed) {
        this.compressed = compressed;
    }

    public static LitheString of(String input) {
        return new LitheString(input);
    }

    public static LitheString fromBytes(byte[] compressed) {
        return new LitheString(compressed);
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || (obj instanceof LitheString && Arrays.equals(((LitheString) obj).compressed, this.compressed));
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(compressed);
    }

    /**
     * Returns the zipped byte[] content
     *
     * @return the zipped byte[] content
     */
    public byte[] getBytes() {
        return compressed;
    }

    /**
     * Returns the corresponding String content
     *
     * @return the corresponding String content
     */
    public String getString() {
        return unzip(compressed);
    }

    /**
     * Compresses the string as best as it can
     *
     * @param input
     * @return the compressed byte[]
     */
    public static byte[] zip(String input) {
        byte[] z0 = input.getBytes(StandardCharsets.UTF_8);
        return zipUTF8(z0);
    }

    /**
     * Compresses the string already in UTF-8 form as best as it can
     *
     * @param utf8Input
     * @return the compressed byte[]
     */
    public static byte[] zipUTF8(byte[] utf8Input) {
        int len = utf8Input.length;
        if (len <= 64) {
            byte[] z1 = z1UTF8(utf8Input);
            byte[] z2 = z2UTF8(utf8Input);
            return shortest(utf8Input, z1, z2);
        }
        if (len <= 512) {
            byte[] z1 = z1UTF8(utf8Input);
            byte[] z2 = z2UTF8(utf8Input);
            byte[] z3 = z3UTF8(utf8Input);
            return shortest(utf8Input, z1, z2, z3);
        }
        return z3UTF8(utf8Input);
    }

    /**
     * Compresses the string and checks if the encoding is correct, throwing exception if it didn't work
     *
     * @param input
     * @return the compressed byte[]
     */
    public static byte[] secureZip(String input) {
        byte[] zipped = zip(input);
        String unzipped = unzip(zipped);
        if (!input.equals(unzipped)) {
            throw new IllegalArgumentException("Error in encoding String '" + (input.length() > 100 ? input.substring(0, 100) + "..." : input) + "'");
        }
        return zipped;
    }

    /**
     * Compresses the string using a custom encoding with 5 bits for a-z and space charactes, and adds 3 bits to every other UTF-8 character
     *
     * @param input
     * @return the compressed byte[]
     */
    public static byte[] z1(String input) {
        return z1UTF8(input.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Like z1, but with an UTF-8 encoded string as input
     *
     * @param utf8Input
     * @return the compressed byte[]
     */
    public static byte[] z1UTF8(byte[] utf8Input) {
        BitWriter output = new BitWriter();
        output.write01("100");

        boolean caps = false;
        try (ByteArrayInputStream bais = new ByteArrayInputStream(utf8Input)) {
            int in;
            while ((in = bais.read()) != -1) {
                byte byt = (byte) in;
                int nExtraByte = getNExtraBytes(byt);
                for (int i = 0; i < nExtraByte; i++) {
                    bais.read();
                }
                if (byt >= 97 && byt <= 122) { // lower
                    caps = false;
                    break;

                }
                if (byt >= 65 && byt <= 90) { // upper
                    caps = true;
                    break;
                }
            }
        } catch (IOException e) {

        }
        output.write(caps);

        try (PushbackInputStream bais = new PushbackInputStream(new ByteArrayInputStream(utf8Input))) {
            int in;
            while ((in = bais.read()) != -1) {
                byte byt = (byte) in;
                if (byt >= 97 && byt <= 122) { // lower
                    if (caps) {
                        int in2 = bais.read();
                        if (in2 != -1) {
                            bais.unread(in2); // push it back
                            if (in2 >= 65 && in2 <= 90) { // if the next is upper
                                output.write01("111"); // write this in utf8
                                output.write(byt);
                                continue;
                            }
                        }
                        output.write01("00000");
                        caps = !caps;
                    }
                    output.writeLast5Bits((byte) (byt - 96));

                } else if (byt >= 65 && byt <= 90) { // upper
                    if (!caps) {
                        int in2 = bais.read();
                        if (in2 != -1) {
                            bais.unread(in2); // push it back
                            if (in2 >= 97 && in2 <= 122) { // if the next is lower
                                output.write01("111"); // write this in utf8
                                output.write(byt);
                                continue;
                            }
                        }
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

    /**
     * Returns how many bytes are after this to complete the UTF-8 character
     *
     * @param byt
     * @return the number of extra bytes
     */
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

    /**
     * Returns if byte parameter starts with the sequence of "010..." as written in binaryString
     *
     * @param byt
     * @param binaryString
     * @return if the 010... of binaryString match the start of byt
     */
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

    /**
     * Compresses the string using a modified Huffman encoding
     *
     * @param input
     * @return the compressed byte[]
     */
    public static byte[] z2(String input) {
        return z2UTF8(input.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Like z2, but with an UTF-8 encoded string as input
     *
     * @param input
     * @return the compressed byte[]
     */
    public static byte[] z2UTF8(byte[] input) {
        List<UTF8Char> listChars = new ArrayList<>();
        try (ByteArrayInputStream bais = new ByteArrayInputStream(input)) {
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

        Map<UTF8Char, Integer> objectFreqs = Huffer.makeFreqs(listChars);
        {
            int howMany = 0;
            for (Iterator<Map.Entry<UTF8Char, Integer>> it = objectFreqs.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<UTF8Char, Integer> next = it.next();
                if (next.getValue() == 1) {
                    it.remove();
                    howMany++;
                }
            }
            if (howMany > 0) {
                objectFreqs.put(UTF8Char.getInvalidChar(), howMany);
            }
        }
        Map<UTF8Char, String> huff = Huffer.makeMap(objectFreqs);

        List<Map.Entry<UTF8Char, String>> listHuff = new ArrayList<>();
        for (Map.Entry<UTF8Char, String> e : huff.entrySet()) {
            listHuff.add(new AbstractMap.SimpleEntry<UTF8Char, String>(e.getKey(), e.getValue()));
        }
        Collections.sort(listHuff, new Comparator<Map.Entry<UTF8Char, String>>() {
            @Override
            public int compare(Map.Entry<UTF8Char, String> o1, Map.Entry<UTF8Char, String> o2) {
                return Integer.compare(o1.getValue().length(), o2.getValue().length());
            }
        });

        BitWriter output = innerZ2(listChars, 0, huff, listHuff);
        int spareBits = output.getSpareBits();
        if (spareBits > 0) {
            output = innerZ2(listChars, spareBits, huff, listHuff);
        }
        output.close();
        return output.toByteArray();
    }

    /**
     * Inner workings of z2
     *
     * @param listChars
     * @param spareBits
     * @param huff
     * @param listHuff
     * @return
     */
    private static BitWriter innerZ2(List<UTF8Char> listChars, int spareBits, Map<UTF8Char, String> huff, List<Map.Entry<UTF8Char, String>> listHuff) {
        BitWriter output = new BitWriter();
        output.write01("1010");
        for (int i = 0; i < spareBits; i++) {
            output.write01("0");
        }
        output.write01("1");

        int length = 0;
        for (Map.Entry<UTF8Char, String> e : listHuff) {
            int difference = e.getValue().length() - length;
            if (difference > 0) {
                for (int i = 0; i < difference; i++) {
                    output.write01("0");
                }
                output.write01("1");
                length = e.getValue().length();
            } else {
                output.write01("10");
            }
            output.write01(e.getValue());
            if (e.getKey().isInvalid()) {
                output.write01("10");
            } else {
                for (int i = 0; i < e.getKey().getBytes().length; i++) {
                    output.write(e.getKey().getBytes()[i]);
                }
            }
        }
        output.write01("11");
        for (UTF8Char c : listChars) {
            String s = huff.get(c);
            if (s != null) {
                output.write01(s);

            } else {
                output.write01(huff.get(UTF8Char.getInvalidChar()));
                for (byte b : c.getBytes()) {
                    output.write(b);
                }
            }
        }
        return output;
    }

    /**
     * Compresses the string with 1 byte of header + standard gzip encoding of the UTF-8 content
     *
     * @param input
     * @return the compressed byte[]
     */
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

    /**
     * Like z3, but with an UTF-8 encoded string as input
     *
     * @param input
     * @return the compressed byte[]
     */
    public static byte[] z3UTF8(byte[] input) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write(0b10111111);
        try {
            try (GZIPOutputStream writer = new GZIPOutputStream(output)) {
                writer.write(input, 0, input.length);
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

    /**
     * Uncompresses the compressed content with the right algorithm
     *
     * @param content
     * @return the original string
     */
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
        if (startsWith(content[0], "1010")) {
            return unzip2(content);
        }
        if ((content[0] & 0xFF) == 0b10111111) {
            return unzip3(content);
        }
        return new String(content, StandardCharsets.UTF_8);
    }

    /**
     * Uncompresses the compressed content using type1 algorithm
     *
     * @param content
     * @return the original string
     */
    private static String unzip1(byte[] content) {
        BitReader bitReader = new BitReader(content);
        bitReader.advance(3);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        boolean caps = bitReader.read(1) == 1;
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

    /**
     * Uncompresses the compressed content using type2 algorithm
     *
     * @param content
     * @return the original string
     */
    private static String unzip2(byte[] content) {
        BitReader bitReader = new BitReader(content);
        bitReader.advance(4);
        while (bitReader.read(1) == 0) ;

        int keyLength = 0;
        Trie01<UTF8Char> trie = new Trie01<>();
        Map<String, String> mmm = new HashMap<>();
        while (!bitReader.isClosed()) {
            if (bitReader.peek01("0")) {
                while (bitReader.read(1) == 0) {
                    keyLength++;
                }
            } else if (bitReader.peek01("10")) {
                bitReader.advance(2);
            } else if (bitReader.peek01("11")) {
                bitReader.advance(2);
                break;
            }
            String key = bitReader.readAsString(keyLength);
            if (bitReader.peek01("10")) {
                bitReader.advance(2);
                UTF8Char u = UTF8Char.getInvalidChar();
                trie.add(key, u);
                mmm.put(key, u.asString());
            } else {
                byte read = bitReader.read();

                int nExtraBytes = getNExtraBytes(read);
                byte[] bytes = new byte[nExtraBytes + 1];
                bytes[0] = read;
                for (int i = 0; i < nExtraBytes; i++) {
                    bytes[i + 1] = bitReader.read();
                }
                UTF8Char u = new UTF8Char(bytes);
                trie.add(key, u);
                mmm.put(key, u.asString());
            }
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while (!bitReader.isClosed()) {
            Trie01.Scanner<UTF8Char> scanner = trie.scan(bitReader.read01Char());
            while (!scanner.hasValue()) {
                scanner.scan(bitReader.read01Char());
            }
            UTF8Char value = scanner.getValue();
            if (value.isInvalid()) {
                byte read = bitReader.read();

                int nExtraBytes = getNExtraBytes(read);
                byte[] bytes = new byte[nExtraBytes + 1];
                bytes[0] = read;
                for (int i = 0; i < nExtraBytes; i++) {
                    bytes[i + 1] = bitReader.read();
                }
                UTF8Char u = new UTF8Char(bytes);
                for (byte b : u.getBytes()) {
                    baos.write(b);
                }
            } else {
                for (byte b : value.getBytes()) {
                    baos.write(b);
                }
            }
        }
        return new String(baos.toByteArray(), StandardCharsets.UTF_8);
    }

    /**
     * Uncompresses the compressed content using type3 algorithm
     *
     * @param content
     * @return the original string
     */
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

    /**
     * Returns the shortest between these byte[]
     *
     * @param bytes
     * @return the shortest byte[]
     */
    private static byte[] shortest(byte[]... bytes) {
        byte[] shortest = bytes[0];
        for (int i = 1; i < bytes.length; i++) {
            if (bytes[i].length < shortest.length) {
                shortest = bytes[i];
            }
        }
        return shortest;
    }

    /**
     * Returns the value of the n-th bit of the byte
     *
     * @param value
     * @param n
     * @return the value of the n-th bit of the byte
     */
    private static boolean getNBitValue(byte value, byte n) {
        return (value & (1 << n)) != 0;
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

        public String readAsString(int numberOfBits) {
            StringBuilder sb = new StringBuilder();
            for (int bit = numberOfBits; bit-- > 0; ) {
                if (bytePos >= bytes.length) {
                    return null; //force return null because the stream ends
                }
                if (getNBitValue(bytes[bytePos], curpos)) {
                    sb.append('1');
                } else {
                    sb.append('0');
                }
                if (curpos == 0) {
                    bytePos++;
                    curpos = START;
                } else {
                    curpos--;
                }
            }
            return sb.toString();
        }

        public char read01Char() {
            if (bytePos >= bytes.length) {
                return 0; //force return 0 because the stream ends
            }
            char c;
            if (getNBitValue(bytes[bytePos], curpos)) {
                c = '1';
            } else {
                c = '0';
            }
            if (curpos == 0) {
                bytePos++;
                curpos = START;
            } else {
                curpos--;
            }
            return c;
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

            public int compareTo(HuffmanTree<T> o) {
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

        private static <T> HuffmanTree<T> makeHuffmanTree(Map<T, Integer> objFreqs) {
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

        private static <T> Map<T, Integer> makeFreqs(Collection<T> input) {
            Map<T, Integer> objectFreqs = new HashMap<>();
            for (T obj : input) {
                Integer count = objectFreqs.get(obj);
                objectFreqs.put(obj, count == null ? 1 : count + 1);
            }
            return objectFreqs;
        }

        private static <T> Map<T, String> makeMap(Map<T, Integer> objectFreqs) {
            Map<T, String> map = new LinkedHashMap<T, String>();
            if (objectFreqs.size() == 1) {
                map.put(objectFreqs.entrySet().iterator().next().getKey(), "1");
            } else {
                toMap(makeHuffmanTree(objectFreqs), new StringBuilder(), map);
            }
            return map;
        }

    }

    private static class UTF8Char {
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
                for (int bit = 8; bit-- > 0; ) {
                    sb.append(((b & (1 << bit)) != 0) ? '1' : '0');
                }
                sb.append(' ');
            }
            return sb.toString();
        }

        public static UTF8Char getInvalidChar() {
            if (invalidChar == null) {
                invalidChar = new UTF8Char(new byte[]{(byte) 0b10000000});
            }
            return invalidChar;
        }

        public boolean isInvalid() {
            return bytes.length == 1 && bytes[0] == (byte) 0b10000000;
        }
    }

    private static class Trie01<T> {

        private Trie01<T>[] chldrn = null;

        private T value = null;

        public void add(String s, T value) {
            privateAdd(s, value, 0);
        }

        @SuppressWarnings("unchecked")
        private void privateAdd(String s, T value2, int i) {
            if (i < s.length()) {
                char charati = s.charAt(i);
                int index = charati == '0' ? 0 : charati == '1' ? 1 : -1;
                if (chldrn == null) chldrn = (Trie01<T>[]) new Trie01[2];
                if (chldrn[index] == null) chldrn[index] = new Trie01<>();
                chldrn[index].privateAdd(s, value2, i + 1);
            } else {
                value = value2;
            }
        }

        public Scanner<T> scan(char c) {
            Scanner<T> sc = new Scanner<T>(this);
            sc.scan(c);
            return sc;
        }

        private static class Scanner<T> {

            private Trie01<T> curNode;

            private Scanner(Trie01<T> start) {
                curNode = start;
            }

            public boolean hasValue() {
                return curNode.chldrn == null;
            }

            public void scan(char c) {
                curNode = curNode.chldrn[c == '0' ? 0 : c == '1' ? 1 : -1];
            }

            public T getValue() {
                return curNode.value;
            }

        }

    }

}