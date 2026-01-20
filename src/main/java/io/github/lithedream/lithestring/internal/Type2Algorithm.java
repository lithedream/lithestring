package io.github.lithedream.lithestring.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

class Type2Algorithm {
    /**
     * Compresses the string using a modified Huffman encoding
     *
     * @param input
     * @return the compressed byte[]
     */
    static byte[] z2(String input) {
        return z2UTF8(input.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Like z2, but with an UTF-8 encoded string as input
     *
     * @param input
     * @return the compressed byte[]
     */
    static byte[] z2UTF8(byte[] input) {
        List<UTF8Char> listChars = new ArrayList<>();
        try (ByteArrayInputStream bais = new ByteArrayInputStream(input)) {
            int in;
            while ((in = bais.read()) != -1) {
                byte byt = (byte) in;
                int nExtraByte = Utils.getNExtraBytes(byt);

                byte[] utf8Bytes = new byte[nExtraByte + 1];
                utf8Bytes[0] = byt;

                for (int i = 0; i < nExtraByte; i++) {
                    utf8Bytes[i + 1] = (byte) bais.read();
                }
                listChars.add(new UTF8Char(utf8Bytes));
            }
        } catch (IOException e) {

        }

        Map<UTF8Char, Integer> objectFreqs = Type2Huffer.makeFreqs(listChars);
        {
            int howMany = 0;
            for (Iterator<Map.Entry<UTF8Char, Integer>> it = objectFreqs.entrySet().iterator(); it.hasNext();) {
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
        Map<UTF8Char, String> huff = Type2Huffer.makeMap(objectFreqs);

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
    private static BitWriter innerZ2(List<UTF8Char> listChars, int spareBits, Map<UTF8Char, String> huff,
            List<Map.Entry<UTF8Char, String>> listHuff) {
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
     * Uncompresses the compressed content using type2 algorithm
     *
     * @param content
     * @return the original string
     */
    static String unzip2(byte[] content) {
        BitReader bitReader = new BitReader(content);
        bitReader.advance(4);
        while (bitReader.read(1) == 0)
            ;

        int keyLength = 0;
        Type2Trie01<UTF8Char> trie = new Type2Trie01<>();
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

                int nExtraBytes = Utils.getNExtraBytes(read);
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
            Type2Trie01.Scanner<UTF8Char> scanner = trie.scan(bitReader.read01Char());
            while (!scanner.hasValue()) {
                scanner.scan(bitReader.read01Char());
            }
            UTF8Char value = scanner.getValue();
            if (value.isInvalid()) {
                byte read = bitReader.read();

                int nExtraBytes = Utils.getNExtraBytes(read);
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

}
