package net.smackem.zlang.emit.bytecode;

import net.smackem.zlang.emit.ir.Program;
import net.smackem.zlang.symbols.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ByteCodeReader {
    private static final Logger log = LoggerFactory.getLogger(ByteCodeReader.class);
    private static final int heapEntryHeaderSize = 12;
    private static final int heapReservedBytes = 16;

    private ByteCodeReader() { }

    public static Map<String, Object> readGlobals(ByteBuffer zl, Program program) {
        final int codeSize = zl.getInt(4);
        final int constSize = zl.getInt(8);
        final int globalOffset = 20 + codeSize + constSize;
        final int globalSize = zl.getInt(12);
        final int heapOffset = globalOffset + globalSize;
        final Map<String, Object> globals = new HashMap<>();
        ByteCodeReader.readObject(zl, globalOffset, heapOffset + heapReservedBytes, program.globals(), globals);
        return globals;
    }

    private static void readObject(ByteBuffer buf, int offset, int heapOffset, Collection<? extends Symbol> symbols, Map<String, Object> out) {
        for (final Symbol symbol : symbols) {
            if (symbol instanceof VariableSymbol == false) {
                log.warn("symbol '" + symbol.name() + "' is not a variable");
            }
            final Object value = readValue(buf, offset, heapOffset, symbol);
            out.put(symbol.name(), value);
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
        if (type instanceof Scope scope) {
            final int ref = buf.getInt(index);
            final Map<String, Object> obj = new HashMap<>();
            readObject(buf, heapOffset + heapEntryHeaderSize + ref, heapOffset, scope.symbols(), obj);
            return obj;
        }
        if (type instanceof ArrayType) {
            return null;
        }
        throw new UnsupportedOperationException("unsupported type '" + type + "'");
    }
}
