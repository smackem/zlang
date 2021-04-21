package net.smackem.zlang.emit.bytecode;

import net.smackem.zlang.emit.ir.Program;
import net.smackem.zlang.symbols.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ByteCodeReader {
    private static final Logger log = LoggerFactory.getLogger(ByteCodeReader.class);

    private ByteCodeReader() { }

    public static Map<String, Object> readGlobals(ByteBuffer zl, Program program) {
        final int codeSize = zl.getInt(4);
        final int constSize = zl.getInt(8);
        final int globalOffset = ByteCode.HEADER_SIZE + codeSize + constSize;
        final int globalSize = zl.getInt(12);
        final int heapOffset = globalOffset + globalSize;
        log.info("ZL: codeSize={} constSize={} globalSize={} heapOffset={}",
                codeSize, constSize, globalSize, heapOffset);
        final Map<String, Object> globals = new HashMap<>();
        readObject(zl, globalOffset, heapOffset, program.globals(), globals);
        return globals;
    }

    private static void readObject(ByteBuffer buf, int offset, int heapOffset, Collection<? extends Symbol> symbols, Map<String, Object> out) {
        for (final Symbol symbol : symbols) {
            if (symbol instanceof VariableSymbol) {
                final Object value = readValue(buf, offset, heapOffset, symbol);
                out.put(symbol.name(), value);
            }
        }
    }

    private static Object readValue(ByteBuffer buf, int offset, int heapOffset, Symbol symbol) {
        final Type type = symbol.type();
        final int index = offset + symbol.address();
        if (type == BuiltInTypeSymbol.INT) {
            return buf.getInt(index);
        }
        if (type == BuiltInTypeSymbol.BOOL) {
            return buf.getInt(index) != 0;
        }
        if (type == BuiltInTypeSymbol.BYTE) {
            return buf.get(index);
        }
        if (type == BuiltInTypeSymbol.FLOAT) {
            return buf.getDouble(index);
        }
        if (type == BuiltInTypeSymbol.RUNTIME_PTR) {
            return buf.getLong(index);
        }
        if (type == BuiltInTypeSymbol.OBJECT) {
            final int value = buf.getInt(index);
            return value == 0 ? null : value; // usually, only nil has the base type OBJECT
        }
        if (type instanceof Scope scope) {
            final int ref = buf.getInt(index);
            if (ref == 0) {
                return null;
            }
            final Map<String, Object> obj = new HashMap<>();
            readObject(buf, heapOffset + ref + ByteCode.HEAP_ENTRY_HEADER_SIZE, heapOffset, scope.symbols(), obj);
            return obj;
        }
        if (type instanceof ArrayType) {
            final int ref = buf.getInt(index);
            if (ref == 0) {
                return null;
            }
            final int elementTypeId = buf.getInt(heapOffset + ref);
            final BuiltInTypeSymbol elementType = BuiltInTypeSymbol.fromId(elementTypeId);
            final int dataSize = buf.getInt(heapOffset + ref + 8);
            final int count = dataSize / elementType.byteSize();
            final int arrayOffset = heapOffset + ref + ByteCode.HEAP_ENTRY_HEADER_SIZE;
            return readArray(buf, arrayOffset, elementType, count);
        }
        throw new UnsupportedOperationException("unsupported type '" + type + "'");
    }

    private static Object readArray(ByteBuffer buf, int offset, BuiltInTypeSymbol elementType, int count) {
        if (elementType == BuiltInTypeSymbol.INT
                || elementType == BuiltInTypeSymbol.OBJECT
                || elementType == BuiltInTypeSymbol.BOOL) {
            final IntBuffer intBuf = buf.slice(offset, count * elementType.byteSize()).order(ByteOrder.nativeOrder()).asIntBuffer();
            final int[] data = new int[count];
            intBuf.get(0, data);
            return data;
        }
        if (elementType == BuiltInTypeSymbol.BYTE) {
            final byte[] data = new byte[count];
            buf.get(offset, data);
            return data;
        }
        if (elementType == BuiltInTypeSymbol.FLOAT) {
            final DoubleBuffer floatBuf = buf.slice(offset, count * elementType.byteSize()).order(ByteOrder.nativeOrder()).asDoubleBuffer();
            final double[] data = new double[count];
            floatBuf.get(0, data);
            return data;
        }
        if (elementType == BuiltInTypeSymbol.RUNTIME_PTR) {
            final LongBuffer longBuf = buf.slice(offset, count * elementType.byteSize()).order(ByteOrder.nativeOrder()).asLongBuffer();
            final long[] data = new long[count];
            longBuf.get(0, data);
            return data;
        }
        throw new UnsupportedOperationException("unsupported type '" + elementType + "'");
    }
}
