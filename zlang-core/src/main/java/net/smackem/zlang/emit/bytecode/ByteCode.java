package net.smackem.zlang.emit.bytecode;

public final class ByteCode {
    private ByteCode() { }

    public static final byte MAJOR_VERSION = 0;
    public static final byte MINOR_VERSION = 1;
    public static final int HEADER_SIZE = 40;
    public static final int HEAP_ENTRY_HEADER_SIZE = 16;
    public static final int HEAP_ENTRY_TYPE_META_FLAG = 0x80000000;
    public static final int HEAP_RESERVED_BYTES = 0x10;
    public static final int TYPE_META_HEADER_SIZE = 16;
    public static final int FREE_HEAP_ENTRY_REF_COUNT = 0x80000000;
}
