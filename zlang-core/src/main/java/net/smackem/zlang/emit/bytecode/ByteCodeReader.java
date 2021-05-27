package net.smackem.zlang.emit.bytecode;

import net.smackem.zlang.emit.ir.Program;
import net.smackem.zlang.symbols.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ByteCodeReader {
    private static final Logger log = LoggerFactory.getLogger(ByteCodeReader.class);

    private ByteCodeReader() { }

    public static Map<String, Object> readGlobals(ByteBuffer zl, int heapOffset, Program program) {
        final int codeSize = zl.getInt(4);
        final int constSize = zl.getInt(8);
        final int globalOffset = ByteCode.HEADER_SIZE + codeSize + constSize;
        final int globalSize = zl.getInt(12);
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
        if (type == BuiltInType.INT.type()) {
            return buf.getInt(index);
        }
        if (type == BuiltInType.BOOL.type()) {
            return buf.getInt(index) != 0;
        }
        if (type == BuiltInType.BYTE.type()) {
            return buf.get(index);
        }
        if (type == BuiltInType.FLOAT.type()) {
            return buf.getDouble(index);
        }
        if (type == BuiltInType.RUNTIME_PTR.type()) {
            return buf.getLong(index);
        }
        if (type == BuiltInType.OBJECT.type()) {
            final int value = buf.getInt(index);
            return value == 0 ? null : value; // usually, only nil has the base type OBJECT
        }
        if (type instanceof ArrayType) {
            final int ref = buf.getInt(index);
            if (ref == 0) {
                return null;
            }
            final int elementTypeNumber = buf.getInt(heapOffset + ref);
            final RegisterTypeId elementTypeId = RegisterTypeId.fromNumber(elementTypeNumber);
            final int dataSize = buf.getInt(heapOffset + ref + 8);
            final int count = dataSize / elementTypeId.byteSize();
            final int arrayOffset = heapOffset + ref + ByteCode.HEAP_ENTRY_HEADER_SIZE;
            final Object array = readArray(buf, arrayOffset, elementTypeId, count);
            return type instanceof StringType
                    ? new String((byte[]) array, 0, count - 1, StandardCharsets.US_ASCII)
                    : array;
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
        throw new UnsupportedOperationException("unsupported type '" + type + "'");
    }

    private static Object readArray(ByteBuffer buf, int offset, RegisterTypeId elementTypeId, int count) {
        if (elementTypeId == RegisterTypeId.Int32 || elementTypeId == RegisterTypeId.Ref) {
            final IntBuffer intBuf = buf.slice(offset, count * elementTypeId.byteSize()).order(ByteOrder.nativeOrder()).asIntBuffer();
            final int[] data = new int[count];
            intBuf.get(0, data);
            return data;
        }
        if (elementTypeId == RegisterTypeId.Unsigned8) {
            final byte[] data = new byte[count];
            buf.get(offset, data);
            return data;
        }
        if (elementTypeId == RegisterTypeId.Float64) {
            final DoubleBuffer floatBuf = buf.slice(offset, count * elementTypeId.byteSize()).order(ByteOrder.nativeOrder()).asDoubleBuffer();
            final double[] data = new double[count];
            floatBuf.get(0, data);
            return data;
        }
        if (elementTypeId == RegisterTypeId.NativePtr) {
            final LongBuffer longBuf = buf.slice(offset, count * elementTypeId.byteSize()).order(ByteOrder.nativeOrder()).asLongBuffer();
            final long[] data = new long[count];
            longBuf.get(0, data);
            return data;
        }
        throw new UnsupportedOperationException("unsupported type '" + elementTypeId + "'");
    }
}
