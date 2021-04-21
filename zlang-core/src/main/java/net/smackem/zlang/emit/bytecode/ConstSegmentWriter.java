package net.smackem.zlang.emit.bytecode;

import net.smackem.zlang.emit.ir.FunctionCode;
import net.smackem.zlang.emit.ir.Label;
import net.smackem.zlang.symbols.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collection;
import java.util.Map;

class ConstSegmentWriter extends NativeValueWriter {
    private static final int typeNameByteLength = 64;
    private static final int maxImplementedInterfaces = 8;

    ConstSegmentWriter() {
        super(new ByteArrayOutputStream());
    }

    // #define MAX_IMPLEMENTED_INTERFACES 8
    // typedef struct type_meta {
    //     /// the name of the type as a zero-terminated string
    //     char name[64]; // always 64 bytes (padded with zeroes)
    //
    //     /// number of implemented interfaces
    //     size_t implemented_interfaces_count;
    //
    //     /// addresses of implemented interfaces in const segment
    //     addr_t implemented_interfaces[MAX_IMPLEMENTED_INTERFACES];
    //
    //     /// zero-terminated list of field types -
    //     Type field_types[4]; // zero-terminated (last item is TYPE_Void), 4 bytes for padding
    // } TypeMeta;

    public void writeType(InterfaceSymbol symbol) throws IOException {
        symbol.setAddress(bytesWritten());
        writeString(symbol.name(), typeNameByteLength);
        writeInt32(0);
        for (int i = 0; i < maxImplementedInterfaces; i++) {
            writeAddr(0);
        }
        // no fields to write...
    }

    public void writeType(StructSymbol symbol) throws IOException {
        symbol.setAddress(bytesWritten());
        writeString(symbol.name(), typeNameByteLength);
        final int implCount = symbol.implementedInterfaces().size();
        writeInt32(implCount);
        for (final Type ifc : symbol.implementedInterfaces()) {
            writeAddr(((InterfaceSymbol) ifc).address());
        }
        for (int i = implCount; i < maxImplementedInterfaces; i++) {
            writeAddr(0);
        }
        for (final Symbol field : symbol.symbols()) {
            if (field instanceof FieldSymbol) {
                writeByte((byte) field.type().primitive().id());
            }
        }
        writeByte((byte) 0); // zero-terminated
    }

    // typedef struct function_meta {
    //     /// the offset of the function's first instruction
    //     addr_t pc;
    //
    //     /// the number of local variables of the function
    //     int local_count;
    //
    //     /// the number of arguments accepted by the function
    //     int arg_count;
    //
    //     /// the return type of the function
    //     Type ret_type;
    //
    //     /// zero-terminated string containing the function name. when contained in the constant segment,
    //     /// the length of the string may be variable.
    //     char name[32];
    // } FunctionMeta;

    public void writeFunction(FunctionSymbol symbol) throws IOException {
        symbol.setAddress(bytesWritten());
        writeAddr(0); // pc - fixup later
        writeInt32(symbol.localCount());
        writeInt32(symbol instanceof MethodSymbol ? symbol.symbols().size() + 1 : symbol.symbols().size()); // + 1 for self
        writeByte(symbol.type() != null ? (byte) symbol.type().primitive().id() : 0);
        writeString(symbol.name());
    }

    public byte[] fixup(Map<FunctionSymbol, FunctionCode> codeMap) throws IOException {
        flush();
        final byte[] bytes = ((ByteArrayOutputStream) outputStream()).toByteArray();
        final ByteBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.nativeOrder());
        for (final var entry : codeMap.entrySet()) {
            final int offset = entry.getKey().address();
            final FunctionCode fc = entry.getValue();
            buf.putInt(offset, fc.firstInstr().address());
        }
        return bytes;
    }
}
