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

    private static final int typeMetaHeaderSize = 16;

    ConstSegmentWriter() {
        super(new ByteArrayOutputStream());
    }

    // typedef struct virtual_function_meta {
    //     addr_t declaring_type;
    // } VirtualFunctionMeta;

    public void writeType(InterfaceSymbol symbol) throws IOException {
        writeType(symbol, List.of(), Map.of(), List.of());

        // write interface methods
        for (final Symbol methodSymbol : symbol.symbols()) {
            if (methodSymbol instanceof InterfaceMethodSymbol ifcMethod) {
                ifcMethod.setAddress(bytesWritten());
                writeInt32(symbol.address());
            }
        }
    }

    public void writeType(AggregateTypeSymbol symbol) throws IOException {
        writeType(symbol, symbol.implementedInterfaces(), symbol.buildVirtualTable(), symbol.symbols());
    }

    // typedef struct type_meta {
    //     addr_t name_offset;
    //     addr_t implemented_interfaces_offset;
    //     addr_t vtable_offset;
    //     addr_t field_types_offset;
    //     byte_t data[4];
    // } TypeMeta;

    private void writeType(Symbol symbol,
                           Collection<Type> implementedInterfaces,
                           Map<InterfaceMethodSymbol, MethodSymbol> vtable,
                           Collection<Symbol> fields) throws IOException {
        symbol.setAddress(bytesWritten());
        final ChunkWriter chunk = new ChunkWriter();
        final int nameOffset, interfacesOffset, vtableOffset, fieldsOffset;
        try (chunk) {
            chunk.writeAddr(0); // name_offset
            chunk.writeAddr(0); // implemented_interfaces_offset
            chunk.writeAddr(0); // vtable_offset
            chunk.writeAddr(0); // fields_offset
            assert chunk.bytesWritten() == typeMetaHeaderSize;
            chunk.setMark();
            nameOffset = chunk.bytesWrittenSinceMark();
            chunk.writeString(((Type) symbol).typeName());
            interfacesOffset = chunk.bytesWrittenSinceMark();
            for (final Type ifc : implementedInterfaces) {
                chunk.writeAddr(((InterfaceSymbol) ifc).address());
            }
            chunk.writeAddr(0); // zero-terminate name
            vtableOffset = chunk.bytesWrittenSinceMark();
            for (final var vtableEntry : vtable.entrySet()) {
                assert vtableEntry.getKey().address() != 0;
                chunk.writeAddr(vtableEntry.getKey().address());
                chunk.writeAddr(0); // method addresses need to be fixed up later
            }
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
    //     addr_t pc;
    //     int local_count;
    //     int arg_count;
    //     Type ret_type;
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

    public byte[] fixup(Collection<Type> types, Map<FunctionSymbol, FunctionCode> codeMap) throws IOException {
        flush();
        final byte[] bytes = ((ByteArrayOutputStream) outputStream()).toByteArray();
        final ByteBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.nativeOrder());

        // fixup aggregate type vtables
        for (final Type type : types) {
            if (type instanceof AggregateTypeSymbol == false) {
                continue;
            }
            final AggregateTypeSymbol aggregate = (AggregateTypeSymbol) type;
            final IntBuffer typeMetaHeader = buf.slice(aggregate.address(), typeMetaHeaderSize).order(ByteOrder.nativeOrder()).asIntBuffer();
            int vtableEntryOffset = aggregate.address() + typeMetaHeaderSize + typeMetaHeader.get(2);
            for (final var vtableEntry : aggregate.buildVirtualTable().entrySet()) {
                assert buf.getInt(vtableEntryOffset) == vtableEntry.getKey().address();
                assert vtableEntry.getValue().address() != 0;
                buf.putInt(vtableEntryOffset + 4, vtableEntry.getValue().address());
                vtableEntryOffset += 8;
            }
        }

        // fixup function pcs
        for (final var entry : codeMap.entrySet()) {
            final int offset = entry.getKey().address();
            final FunctionCode fc = entry.getValue();
            buf.putInt(offset, fc.firstInstr().address());
        }
        return bytes;
    }
}
