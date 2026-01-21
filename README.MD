# LitheString

A tiny Java library that compresses short strings without risking bloat.

## Why

Most compressors get worse on short text. LitheString picks the best of multiple strategies at runtime and falls back to plain UTF‑8 when compression wouldn’t help.

## How it works

The encoder chooses the smallest result among:

- Raw UTF‑8 (no overhead)
- A 5‑bit alphabet for `a–z`, `A–Z`, and space (great for short Latin text)
- A Huffman‑based encoding (dictionary header + payload)
- GZIP with a 1‑byte header (best for long or repetitive strings)

The decoder inspects the header to determine the correct algorithm.

## Quick start

```java
String input = "hello world";

// Functional API
byte[] compressed = LitheString.zip(input);
String uncompressed = LitheString.unzip(compressed);

// Value object API
LitheString ls = LitheString.of(input);
byte[] bytes = ls.getBytes();
String text = ls.getString();

assert input.equals(text);
assert bytes.length <= input.getBytes(StandardCharsets.UTF_8).length;
```

## Safety

```java
byte[] compressed = LitheString.secureZip(input); // throws if round‑trip fails
```

## Author

- **lithedream**

## License

MIT
