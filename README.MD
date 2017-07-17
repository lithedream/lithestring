# lithestring

## Synopsis

Java class to compress short (or long) strings

Included in the lithecore library https://github.com/lithedream/lithecore

## Motivation

I dreamed a compression algorithm expecially useful for short strings, without fear of taking up more space than the UTF-8 encoding of the original string.

The compression algorithm chooses the best approach between:
* Plain UTF-8 encoding (as is, without overhead)
* An encoding which uses 5 bits for a-z, A-Z, space, and encodes every other UTF-8 character with 3 bits of overhead (for really short Latin strings)
* An intermediate algorithm based on Huffman encoding (dictionary header, then encoded string)
* 1 byte of overhead then GZIP compression (for long strings)

The decompression algorithm looks if it is a plain UTF-8 encoding or a compressed one, and in the latter case reads the data header to apply the correct decoding algorithm.

## Code Example

```java
String input = ...;
byte[] compressed = LitheString.zip(input); // in the worst case, compressed is the plain UTF-8 encoding of input
String uncompressed = LitheString.unzip(compressed);

if (input.equals(uncompressed)){
    System.out.println("It works!");
} else {
    System.out.println("Please submit a bug for "+input);
}
```

## Author

* **lithedream**

## License

LGPL-2.1
