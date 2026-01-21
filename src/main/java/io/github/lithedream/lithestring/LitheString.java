package io.github.lithedream.lithestring;

import java.io.Serializable;
import java.lang.ref.SoftReference;
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
public final class LitheString implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private final byte[] compressed;
    private final boolean immutable;
    private final boolean cacheEnabled;
    private transient SoftReference<String> cached;

    private LitheString(byte[] compressed, boolean immutable, boolean cacheEnabled) {
        this.compressed = compressed;
        this.immutable = immutable;
        this.cacheEnabled = cacheEnabled;
    }

    /**
     * Creates a compressed instance from a String.
     *
     * @param input the input string
     * @return a new {@code LitheString} instance
     */
    public static LitheString of(String input) {
        return of(input, false, false);
    }

    /**
     * Creates a compressed instance with configurable behaviors.
     *
     * @param input        the input string
     * @param immutable    if true, the compressed bytes are defensively copied and
     *                     {@link #getBytes()}
     *                     returns a copy; if false, the internal array may be
     *                     exposed
     * @param cacheEnabled if true, {@link #getString()} soft-caches the decoded
     *                     string
     * @return a new {@code LitheString} instance
     */
    public static LitheString of(String input, boolean immutable, boolean cacheEnabled) {
        return new LitheString(zip(input), immutable, cacheEnabled);
    }

    /**
     * Creates a {@code LitheString} instance from already-compressed bytes.
     *
     * @param compressed the compressed byte array
     * @return a new {@code LitheString} instance
     */
    public static LitheString fromBytes(byte[] compressed) {
        return fromBytes(compressed, false, false);
    }

    /**
     * Creates a {@code LitheString} instance from already-compressed bytes.
     *
     * @param compressed   the compressed byte array
     * @param immutable    if true, the bytes are defensively copied and
     *                     {@link #getBytes()} returns a copy
     * @param cacheEnabled if true, {@link #getString()} soft-caches the decoded
     *                     string
     * @return a new {@code LitheString} instance
     */
    public static LitheString fromBytes(byte[] compressed, boolean immutable, boolean cacheEnabled) {
        return new LitheString(
                (immutable && compressed != null) ? Arrays.copyOf(compressed, compressed.length) : compressed,
                immutable, cacheEnabled);
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
     * If {@code immutable} is true, this returns a defensive copy; otherwise it
     * returns the internal array.
     */
    public byte[] getBytes() {
        if (immutable && compressed != null) {
            return Arrays.copyOf(compressed, compressed.length);
        }
        return compressed;
    }

    /**
     * Returns the original string by decoding the stored bytes.
     *
     * @return decoded string
     */
    public String getString() {
        if (cacheEnabled) {
            String value = cached == null ? null : cached.get();
            if (value != null) {
                return value;
            }
        }
        String decoded = LitheStringAlgorithm.unzip(compressed);
        if (cacheEnabled) {
            cached = new SoftReference<>(decoded);
        }
        return decoded;
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