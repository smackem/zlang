package net.smackem.zlang.emit.bytecode;

import net.smackem.zlang.symbols.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

class ConstSegmentWriter extends NativeValueWriter {
    private static final int typeNameByteLength = 64;
    private static final int maxImplementedInterfaces = 8;

    public ConstSegmentWriter() {
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
            writeByte((byte) field.type().primitive().id());
        }
    }

    public void writeFunction(FunctionSymbol symbol) throws IOException {
        symbol.setAddress(bytesWritten());
    }

    public byte[] getBytes() {
        return ((ByteArrayOutputStream) outputStream()).toByteArray();
    }
}
