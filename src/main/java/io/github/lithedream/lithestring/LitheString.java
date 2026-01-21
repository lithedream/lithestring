package io.github.lithedream.lithestring;

import java.util.Arrays;

import io.github.lithedream.lithestring.internal.LitheStringAlgorithm;

/**
 * Compact string container that stores a compressed representation of a String.
 *
 * <p>
 * The compression algorithm chooses the smallest output among:
 * raw UTF-8, a 5-bit Latin alphabet encoding, a Huffman-based encoding,
 * and GZIP with a small header. Decoding is automatic and based on a header.
 * </p>
 *
 * <p>
 * Usage:
 * </p>
 * 
 * <pre>{@code
 * String input = "hello world";
 * 
 * LitheString ls = LitheString.of(input);
 * byte[] bytes = ls.getBytes();
 * String text = ls.getString();
 * 
 * assert input.equals(text);
 * assert bytes.length <= input.getBytes(StandardCharsets.UTF_8).length;
 * 
 * }</pre>
 */
public class LitheString {

    private byte[] compressed;

    protected LitheString(String input) {
        this.compressed = zip(input);
    }

    protected LitheString(byte[] compressed) {
        this.compressed = compressed;
    }

    /**
     * Creates a compressed instance from a String.
     *
     * @param input the input string
     * @return a new {@code LitheString} instance
     */
    public static LitheString of(String input) {
        return new LitheString(input);
    }

    /**
     * Creates a {@code LitheString} instance from already-compressed bytes.
     *
     * @param compressed the compressed byte array
     * @return a new {@code LitheString} instance
     */
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
     * Returns the compressed bytes for this instance.
     *
     * @return compressed bytes
     */
    public byte[] getBytes() {
        return compressed;
    }

    /**
     * Returns the original string by decoding the stored bytes.
     *
     * @return decoded string
     */
    public String getString() {
        return LitheStringAlgorithm.unzip(compressed);
    }

    /**
     * Compresses a string and returns the encoded bytes.
     * The output is never larger than the UTF-8 bytes of the input.
     *
     * @param input the input string
     * @return compressed bytes
     */
    public static byte[] zip(String input) {
        return LitheStringAlgorithm.zip(input);
    }

    /**
     * Decompresses the given bytes back into a string.
     *
     * @param content compressed bytes produced by {@link #zip(String)}
     * @return decoded string
     */
    public static String unzip(byte[] content) {
        return LitheStringAlgorithm.unzip(content);
    }

    /**
     * Compresses a string and validates round-trip decoding.
     * Throws an exception if the decoded string differs.
     *
     * @param input the input string
     * @return compressed bytes
     * @throws IllegalArgumentException if the decoded output differs from input
     */
    public static byte[] secureZip(String input) {
        return LitheStringAlgorithm.secureZip(input);
    }

}