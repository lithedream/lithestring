package io.github.lithedream.lithestring.internal;

import java.nio.charset.StandardCharsets;

public class LitheStringAlgorithm {

    /**
     * Compresses the string using the best available encoding.
     * Returns {@code null} if {@code input} is null.
     *
     * @param input the input string
     * @return the compressed byte array, or {@code null} if input is null
     */
    public static byte[] zip(String input) {
        byte[] z0 = input != null ? input.getBytes(StandardCharsets.UTF_8) : null;
        return zipUTF8(z0);
    }

    /**
     * Compresses the string using a specific algorithm.
     * Type values: 1 (type1), 2 (type2), 3 (type3/gzip).
     *
     * @param input the input string
     * @param type  compression type (1, 2, or 3)
     * @return the compressed byte array
     * @throws IllegalArgumentException if {@code type} is not 1, 2, or 3
     */
    public static byte[] zip(String input, int type) {
        switch (type) {
            case 1:
                return Type1Algorithm.z1(input);
            case 2:
                return Type2Algorithm.z2(input);
            case 3:
                return Type3Algorithm.z3(input);
            default:
                throw new IllegalArgumentException("Type " + type + " not valid. Valid values are 1,2,3");
        }
    }

    /**
     * Compresses already UTF-8 encoded input using the best available encoding.
     * Returns {@code null} if {@code utf8Input} is null.
     *
     * @param utf8Input UTF-8 encoded bytes
     * @return the compressed byte array, or {@code null} if input is null
     */
    public static byte[] zipUTF8(byte[] utf8Input) {
        if (utf8Input == null) {
            return null;
        }
        int len = utf8Input.length;
        if (len == 0) {
            return new byte[] {};
        }
        if (len <= 64) {
            byte[] z1 = Type1Algorithm.z1UTF8(utf8Input);
            byte[] z2 = Type2Algorithm.z2UTF8(utf8Input);
            return shortest(utf8Input, z1, z2);
        }
        if (len <= 512) {
            byte[] z1 = Type1Algorithm.z1UTF8(utf8Input);
            byte[] z2 = Type2Algorithm.z2UTF8(utf8Input);
            byte[] z3 = Type3Algorithm.z3UTF8(utf8Input);
            return shortest(utf8Input, z1, z2, z3);
        }
        byte[] z3 = Type3Algorithm.z3UTF8(utf8Input);
        return shortest(utf8Input, z3);
    }

    /**
     * Compresses UTF-8 bytes using a specific algorithm.
     * Type values: 1 (type1), 2 (type2), 3 (type3/gzip).
     *
     * @param utf8Input UTF-8 encoded bytes
     * @param type      compression type (1, 2, or 3)
     * @return the compressed byte array
     * @throws IllegalArgumentException if {@code type} is not 1, 2, or 3
     */
    public static byte[] zipUTF8(byte[] utf8Input, int type) {
        switch (type) {
            case 1:
                return Type1Algorithm.z1UTF8(utf8Input);
            case 2:
                return Type2Algorithm.z2UTF8(utf8Input);
            case 3:
                return Type3Algorithm.z3UTF8(utf8Input);
            default:
                throw new IllegalArgumentException("Type " + type + " not valid. Valid values are 1,2,3");
        }
    }

    /**
     * Compresses and validates round-trip decoding.
     *
     * @param input the input string
     * @return the compressed byte array
     * @throws IllegalArgumentException if decoding does not match the input
     */
    public static byte[] secureZip(String input) {
        byte[] zipped = zip(input);
        String unzipped = unzip(zipped);
        if (zipped != null && unzipped != null && !input.equals(unzipped)) {
            throw new IllegalArgumentException("Error in encoding String '"
                    + (input.length() > 100 ? input.substring(0, 100) + "..." : input) + "'");
        }
        return zipped;
    }

    /**
     * Decompresses the given byte array back into a string.
     * Returns {@code null} if {@code content} is null.
     *
     * @param content compressed bytes produced by {@link #zip(String)}
     * @return the decoded string, or {@code null} if content is null
     */
    public static String unzip(byte[] content) {
        if (content == null) {
            return null;
        }
        if (content.length == 0) {
            return "";
        }
        if (Utils.startsWith(content[0], "100")) {
            return Type1Algorithm.unzip1(content);
        }
        if (Utils.startsWith(content[0], "1010")) {
            return Type2Algorithm.unzip2(content);
        }
        if ((content[0] & 0xFF) == 0b10111111) {
            return Type3Algorithm.unzip3(content);
        }
        return new String(content, StandardCharsets.UTF_8);
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

}