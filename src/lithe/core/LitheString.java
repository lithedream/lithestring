package lithe.core;

public class LitheString {
    private byte[] content;

    public LitheString(String input) {
        this.content = zip(input);
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || (obj != null && (obj instanceof LitheString) && ((LitheString) obj).content.equals(this.content));
    }

    public byte[] getBytes() {
        return content;
    }

    public String getString() {
        return unzip(content);
    }


    public static byte[] zip(String input) {
        //TODO
        return null;
    }

    public static String unzip(byte[] content) {
        //TODO
        return null;
    }


}
