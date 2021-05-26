package net.smackem.zlang.symbols;

public enum RegisterTypeId {
    Void(0, 0),
    Int32(1, 4),
    Float64(2, 8),
    Unsigned8(3, 1),
    String(4, 4),
    Ref(5, 4),
    NativePtr(6, 8);

    private final int id;
    private final int byteSize;

    RegisterTypeId(int id, int byteSize) {
        this.id = id;
        this.byteSize = byteSize;
    }

    public int number() {
        return this.id;
    }

    public int byteSize() {
        return this.byteSize;
    }
}
