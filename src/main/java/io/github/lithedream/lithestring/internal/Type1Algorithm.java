package io.github.lithedream.lithestring.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PushbackInputStream;
import java.nio.charset.StandardCharsets;

class Type1Algorithm {
    /**
     * Compresses the string using a custom encoding with 5 bits for a-z and space
     * charactes, and adds 3 bits to every other UTF-8 character
     *
     * @param input
     * @return the compressed byte[]
     */
    static byte[] z1(String input) {
        return z1UTF8(input.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Like z1, but with an UTF-8 encoded string as input
     *
     * @param utf8Input
     * @return the compressed byte[]
     */
    static byte[] z1UTF8(byte[] utf8Input) {
        BitWriter output = new BitWriter();
        output.write01("100");

        boolean caps = false;
        try (ByteArrayInputStream bais = new ByteArrayInputStream(utf8Input)) {
            int in;
            while ((in = bais.read()) != -1) {
                byte byt = (byte) in;
                int nExtraByte = Utils.getNExtraBytes(byt);
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

                } else if (byt == 32) { // space
                    output.write01("11011");
                } else {
                    output.write01("111");
                    output.write(byt);
                    for (int i = 0; i < Utils.getNExtraBytes(byt); i++) {
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
     * Uncompresses the compressed content using type1 algorithm
     *
     * @param content
     * @return the original string
     */
    static String unzip1(byte[] content) {
        BitReader bitReader = new BitReader(content);
        bitReader.advance(3);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        boolean caps = bitReader.read(1) == 1;
        while (!bitReader.isClosed()) {
            if (bitReader.peek01("111")) {
                bitReader.advance(3);
                byte read = bitReader.read();

                baos.write(read);
                for (int i = 0; i < Utils.getNExtraBytes(read); i++) {
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

}
