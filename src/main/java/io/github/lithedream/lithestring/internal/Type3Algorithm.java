package io.github.lithedream.lithestring.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

class Type3Algorithm {
    /**
     * Compresses the string with 1 byte of header + standard gzip encoding of the
     * UTF-8 content
     *
     * @param input
     * @return the compressed byte[]
     */
    static byte[] z3(String input) {
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
    static byte[] z3UTF8(byte[] input) {
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
     * Uncompresses the compressed content using type3 algorithm
     *
     * @param content
     * @return the original string
     */
    static String unzip3(byte[] content) {
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
}
