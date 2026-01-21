package io.github.lithedream.lithestring;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import io.github.lithedream.lithestring.internal.LitheStringAlgorithm;

class LitheStringTest {

    @Test
    void roundTripBasicCases() {
        assertRoundTrip("");
        assertRoundTrip("hello world");
        assertRoundTrip("Hello World Test");
        assertRoundTrip("Caffe\\u00e8 naive facade");
        assertRoundTrip("smile \\uD83D\\uDE42 rocket \\uD83D\\uDE80");
    }

    @Test
    void fromBytesRoundTrip() {
        String input = "hello world";
        byte[] compressed = LitheString.zip(input);
        LitheString ls = LitheString.fromBytes(compressed);
        assertEquals(input, ls.getString());
        assertArrayEquals(compressed, ls.getBytes());
    }

    @Test
    void compressedNotLargerThanUtf8ForCommonCases() {
        assertNotLargerThanUtf8("hello world");
        assertNotLargerThanUtf8("Hello World Test");
        assertNotLargerThanUtf8("short latin");
    }

    @Test
    void zipByTypeRoundTripStress() {
        String[] samples = {
                "",
                "hello world",
                "Hello World Test",
                "short latin",
                "Caff\u00e8 naive facade",
                "smile \uD83D\uDE42 rocket \uD83D\uDE80",
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                "The quick brown fox jumps over the lazy dog. "
        };

        for (int type = 1; type <= 3; type++) {
            for (String s : samples) {
                byte[] zipped = LitheStringAlgorithm.zip(s, type);
                String unzipped = LitheStringAlgorithm.unzip(zipped);
                assertEquals(s, unzipped, "type=" + type + ", input=" + s);
            }

            for (String s : samples) {
                StringBuffer sb = new StringBuffer();
                for (int i = 0; i < 20; i++) {
                    sb.append(s);
                }
                byte[] zipped = LitheStringAlgorithm.zip(sb.toString(), type);
                String unzipped = LitheStringAlgorithm.unzip(zipped);
                assertEquals(sb.toString(), unzipped, "type=" + type + ", input=" + sb.toString());
            }

            for (String s : samples) {
                StringBuffer sb = new StringBuffer();
                for (int i = 0; i < 200; i++) {
                    sb.append(s);
                }
                byte[] zipped = LitheStringAlgorithm.zip(sb.toString(), type);
                String unzipped = LitheStringAlgorithm.unzip(zipped);
                assertEquals(sb.toString(), unzipped, "type=" + type + ", input=" + sb.toString());
            }
        }
    }

    private static void assertRoundTrip(String input) {
        byte[] compressed = LitheString.zip(input);
        String uncompressed = LitheString.unzip(compressed);
        assertEquals(input, uncompressed);
    }

    private static void assertNotLargerThanUtf8(String input) {
        byte[] compressed = LitheString.zip(input);
        byte[] utf8 = input.getBytes(StandardCharsets.UTF_8);
        assertTrue(compressed.length <= utf8.length);
    }
}
