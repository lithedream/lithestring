package io.github.lithedream.lithestring.internal;

import java.nio.charset.StandardCharsets;

public class LitheStringAlgorithm {

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
        return Type3Algorithm.z3UTF8(utf8Input);
    }

    /**
     * Compresses the string and checks if the encoding is correct, throwing
     * exception if it didn't work
     *
     * @param input
     * @return the compressed byte[]
     */
    public static byte[] secureZip(String input) {
        byte[] zipped = zip(input);
        String unzipped = unzip(zipped);
        if (!input.equals(unzipped)) {
            throw new IllegalArgumentException("Error in encoding String '"
                    + (input.length() > 100 ? input.substring(0, 100) + "..." : input) + "'");
        }
        return zipped;
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