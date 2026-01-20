package io.github.lithedream.lithestring.internal;

class BitReader {
    private static final byte START = 7;
    private byte curpos = START;
    private byte[] bytes;
    private int bytePos = 0;

    public BitReader(byte[] b) {
        bytes = b;
    }

    public byte read() {
        if (curpos == START) {
            if (bytePos >= bytes.length) {
                return 0; // force return 0 because the stream ends
            }
            byte toRet = bytes[bytePos];
            bytePos++;
            return toRet;
        } else {
            byte toRet = 0;
            for (int bit = 8; bit-- > 0;) {
                if (bytePos >= bytes.length) {
                    return 0; // force return 0 because the stream ends
                }
                if (Utils.getNBitValue(bytes[bytePos], curpos)) {
                    toRet |= 1 << bit;
                } else {
                    toRet &= ~(1 << bit);
                }
                if (curpos == 0) {
                    bytePos++;
                    curpos = START;
                } else {
                    curpos--;
                }
            }
            return toRet;

        }
    }

    public byte read(int numberOfBits) {
        byte toRet = 0;
        for (int bit = numberOfBits; bit-- > 0;) {
            if (bytePos >= bytes.length) {
                return 0; // force return 0 because the stream ends
            }
            if (Utils.getNBitValue(bytes[bytePos], curpos)) {
                toRet |= 1 << bit;
            } else {
                toRet &= ~(1 << bit);
            }
            if (curpos == 0) {
                bytePos++;
                curpos = START;
            } else {
                curpos--;
            }
        }
        return toRet;
    }

    public String readAsString(int numberOfBits) {
        StringBuilder sb = new StringBuilder();
        for (int bit = numberOfBits; bit-- > 0;) {
            if (bytePos >= bytes.length) {
                return null; // force return null because the stream ends
            }
            if (Utils.getNBitValue(bytes[bytePos], curpos)) {
                sb.append('1');
            } else {
                sb.append('0');
            }
            if (curpos == 0) {
                bytePos++;
                curpos = START;
            } else {
                curpos--;
            }
        }
        return sb.toString();
    }

    public char read01Char() {
        if (bytePos >= bytes.length) {
            return 0; // force return 0 because the stream ends
        }
        char c;
        if (Utils.getNBitValue(bytes[bytePos], curpos)) {
            c = '1';
        } else {
            c = '0';
        }
        if (curpos == 0) {
            bytePos++;
            curpos = START;
        } else {
            curpos--;
        }
        return c;
    }

    public boolean isClosed() {
        return bytePos >= bytes.length;
    }

    public void advance(int n) {
        for (int i = 0; i < n; i++) {
            if (curpos == 0) {
                bytePos++;
                curpos = START;
            } else {
                curpos--;
            }
        }
    }

    public boolean peek01(String s) {
        int bytePosCopy = bytePos;
        byte curPosCopy = curpos;

        for (int i = 0; i < s.length(); i++) {
            if (bytePosCopy >= bytes.length) {
                return false;
            }
            boolean bitValue = Utils.getNBitValue(bytes[bytePosCopy], curPosCopy);
            boolean iBitIsSet = s.charAt(i) == ('1');
            if (bitValue != iBitIsSet) {
                return false;
            }

            if (curPosCopy == 0) {
                bytePosCopy++;
                curPosCopy = START;
            } else {
                curPosCopy--;
            }
        }
        return true;
    }
}