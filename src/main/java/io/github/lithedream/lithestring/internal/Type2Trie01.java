package io.github.lithedream.lithestring.internal;

class Type2Trie01<T> {

    private Type2Trie01<T>[] chldrn = null;

    private T value = null;

    public void add(String s, T value) {
        privateAdd(s, value, 0);
    }

    @SuppressWarnings("unchecked")
    private void privateAdd(String s, T value2, int i) {
        if (i < s.length()) {
            char charati = s.charAt(i);
            int index = charati == '0' ? 0 : charati == '1' ? 1 : -1;
            if (chldrn == null)
                chldrn = (Type2Trie01<T>[]) new Type2Trie01[2];
            if (chldrn[index] == null)
                chldrn[index] = new Type2Trie01<>();
            chldrn[index].privateAdd(s, value2, i + 1);
        } else {
            value = value2;
        }
    }

    public Scanner<T> scan(char c) {
        Scanner<T> sc = new Scanner<T>(this);
        sc.scan(c);
        return sc;
    }

    static class Scanner<T> {

        private Type2Trie01<T> curNode;

        private Scanner(Type2Trie01<T> start) {
            curNode = start;
        }

        public boolean hasValue() {
            return curNode.chldrn == null;
        }

        public void scan(char c) {
            curNode = curNode.chldrn[c == '0' ? 0 : c == '1' ? 1 : -1];
        }

        public T getValue() {
            return curNode.value;
        }

    }

}