package net.smackem.zlang.emit.bytecode;

import net.smackem.zlang.emit.ir.FunctionCode;
import net.smackem.zlang.symbols.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.Collection;
import java.util.List;
import java.util.Map;

class ConstSegmentWriter extends NativeValueWriter {
    private static final int typeNameByteLength = 64;
    private static final int maxImplementedInterfaces = 8;

    ConstSegmentWriter() {
        super(new ByteArrayOutputStream());
    }

    // typedef struct type_meta {
    //     /// data offset of the name of the type as a zero-terminated string
    //     addr_t name_offset;
    //
    //     /// data offset of the addresses of implemented interfaces in const segment.
    //     /// implemented interfaces is a zero-terminated list of <c>addr_t</c>s
    //     addr_t implemented_interfaces_offset;
    //
    //     /// data offset of the vtable.
    //     /// the vtable is a zero-terminated list of <c>VTableEntry</c> structs. the last struct has all fields zeroed.
    //     addr_t vtable_offset;
    //
    //     /// data offset of a zero-terminated list of field types.
    //     /// field types is a list of <c>Type</c>s, terminated by a <c>TYPE_Void</c>
    //     addr_t field_types_offset;
    //
    //     /// the data chunk
    //     byte_t data[4];
    // } TypeMeta;

    public void writeType(InterfaceSymbol symbol) throws IOException {
        writeType(symbol, List.of(), List.of());
    }

    public void writeType(AggregateTypeSymbol symbol) throws IOException {
        writeType(symbol, symbol.implementedInterfaces(), symbol.symbols());
    }

    private void writeType(Symbol symbol, Collection<Type> implementedInterfaces, Collection<Symbol> fields) throws IOException {
        symbol.setAddress(bytesWritten());
        final ChunkWriter chunk = new ChunkWriter();
        final int nameOffset, interfacesOffset, vtableOffset, fieldsOffset;
        try (chunk) {
            chunk.writeAddr(0); // name_offset
            chunk.writeAddr(0); // implemented_interfaces_offset
            chunk.writeAddr(0); // vtable_offset
            chunk.writeAddr(0); // fields_offset
            chunk.setMark();
            nameOffset = chunk.bytesWrittenSinceMark();
            chunk.writeString(((Type) symbol).typeName());
            interfacesOffset = chunk.bytesWrittenSinceMark();
            for (final Type ifc : implementedInterfaces) {
                chunk.writeAddr(((InterfaceSymbol) ifc).address());
            }
            chunk.writeAddr(0); // zero-terminate name
            vtableOffset = chunk.bytesWrittenSinceMark();
            // TODO write vtable...
            chunk.writeAddr(0); // zero-terminate vtable tuple
            chunk.writeAddr(0);
            fieldsOffset = chunk.bytesWrittenSinceMark();
            for (final Symbol field : fields) {
                if (field instanceof FieldSymbol) {
                    chunk.writeByte((byte) field.type().registerType().id().number());
                }
            }
            chunk.writeByte((byte) 0); // zero-terminate fields
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e); // plain 'Exception' is required by AutoCloseable.close()
        }
        final ByteBuffer byteBuffer = chunk.toByteBuffer();
        byteBuffer.asIntBuffer()
                .put(0, nameOffset)
                .put(1, interfacesOffset)
                .put(2, vtableOffset)
                .put(3, fieldsOffset);
        writeChunk(byteBuffer);
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
        writeByte(symbol.type() != null ? (byte) symbol.type().registerType().id().number() : 0);
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
