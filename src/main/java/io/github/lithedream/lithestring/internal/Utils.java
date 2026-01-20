package io.github.lithedream.lithestring.internal;

class Utils {
    /**
     * Returns how many bytes are after this to complete the UTF-8 character
     *
     * @param byt
     * @return the number of extra bytes
     */
    static int getNExtraBytes(byte byt) {
        int nExtraByte = 0;
        if (!startsWith(byt, "0")) { // 1 byte
            if (startsWith(byt, "110")) { // 2 byte
                nExtraByte = 1;
            } else if (startsWith(byt, "1110")) { // 3 byte
                nExtraByte = 2;
            } else if (startsWith(byt, "11110")) { // 4 byte
                nExtraByte = 3;
            }
        }
        return nExtraByte;
    }

    /**
     * Returns if byte parameter starts with the sequence of "010..." as written in
     * binaryString
     *
     * @param byt
     * @param binaryString
     * @return if the 010... of binaryString match the start of byt
     */
    static boolean startsWith(byte byt, String binaryString) {
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
     * Returns the value of the n-th bit of the byte
     *
     * @param value
     * @param n
     * @return the value of the n-th bit of the byte
     */
    static boolean getNBitValue(byte value, byte n) {
        return (value & (1 << n)) != 0;
    }
}
