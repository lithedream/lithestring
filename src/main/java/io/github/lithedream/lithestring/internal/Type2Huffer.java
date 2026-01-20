package io.github.lithedream.lithestring.internal;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.PriorityQueue;

class Type2Huffer {

    private static class HuffmanTree<T> implements Comparable<HuffmanTree<T>> {

        public final int freq;

        public final HuffmanTree<T> l, r;

        public final T value;

        private final boolean isLeaf;

        public HuffmanTree(HuffmanTree<T> l, HuffmanTree<T> r) {
            freq = l.freq + r.freq;
            this.l = l;
            this.r = r;
            value = null;
            isLeaf = false;
        }

        public HuffmanTree(int freq, T value) {
            this.freq = freq;
            this.value = value;
            l = null;
            r = null;
            isLeaf = true;
        }

        public int compareTo(HuffmanTree<T> o) {
            return freq - o.freq;
        }

    }

    private static <T> void toMap(HuffmanTree<T> tree, StringBuilder prefix, Map<T, String> t) {
        if (tree.isLeaf) {
            t.put(tree.value, prefix.toString());
        } else {
            prefix.append('0');
            toMap(tree.l, prefix, t);
            prefix.deleteCharAt(prefix.length() - 1);

            prefix.append('1');
            toMap(tree.r, prefix, t);
            prefix.deleteCharAt(prefix.length() - 1);
        }
    }

    private static <T> HuffmanTree<T> makeHuffmanTree(Map<T, Integer> objFreqs) {
        PriorityQueue<HuffmanTree<T>> huffmanTrees = new PriorityQueue<HuffmanTree<T>>();
        for (Map.Entry<T, Integer> entry : objFreqs.entrySet()) {
            huffmanTrees.offer(new HuffmanTree<T>(entry.getValue(), entry.getKey()));
        }
        while (huffmanTrees.size() > 1) {
            HuffmanTree<T> l = huffmanTrees.poll();
            HuffmanTree<T> r = huffmanTrees.poll();
            huffmanTrees.offer(new HuffmanTree<T>(l, r));
        }
        return huffmanTrees.poll();
    }

    static <T> Map<T, Integer> makeFreqs(Collection<T> input) {
        Map<T, Integer> objectFreqs = new HashMap<>();
        for (T obj : input) {
            Integer count = objectFreqs.get(obj);
            objectFreqs.put(obj, count == null ? 1 : count + 1);
        }
        return objectFreqs;
    }

    static <T> Map<T, String> makeMap(Map<T, Integer> objectFreqs) {
        Map<T, String> map = new LinkedHashMap<T, String>();
        if (objectFreqs.size() == 1) {
            map.put(objectFreqs.entrySet().iterator().next().getKey(), "1");
        } else {
            toMap(makeHuffmanTree(objectFreqs), new StringBuilder(), map);
        }
        return map;
    }

}