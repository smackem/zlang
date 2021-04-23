package net.smackem.zlang.emit.bytecode;

final class ByteCode {
    private ByteCode() { }

    public static final byte MAJOR_VERSION = 0;
    public static final byte MINOR_VERSION = 1;
    public static final int HEADER_SIZE = 40;
    public static final int HEAP_ENTRY_HEADER_SIZE = 12;
}
