package io.github.lithedream.lithestring;

import java.util.Arrays;

import io.github.lithedream.lithestring.internal.LitheStringAlgorithm;

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
        return this == obj
                || (obj instanceof LitheString && Arrays.equals(((LitheString) obj).compressed, this.compressed));
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
        return LitheStringAlgorithm.unzip(compressed);
    }

    /**
     * Compresses the string as best as it can
     *
     * @param input
     * @return the compressed byte[]
     */
    public static byte[] zip(String input) {
        return LitheStringAlgorithm.zip(input);
    }

    /**
     * Uncompresses the compressed content with the right algorithm
     *
     * @param content
     * @return the original string
     */
    public static String unzip(byte[] content) {
        return LitheStringAlgorithm.unzip(content);
    }

    /**
     * Compresses the string and checks if the encoding is correct, throwing
     * exception if it didn't work
     *
     * @param input
     * @return the compressed byte[]
     */
    public static byte[] secureZip(String input) {
        return LitheStringAlgorithm.secureZip(input);
    }

}