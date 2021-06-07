package net.smackem.zlang.emit.bytecode;

public record HeapEntry(int address, String typeName, int refCount, int dataSize) {
}
