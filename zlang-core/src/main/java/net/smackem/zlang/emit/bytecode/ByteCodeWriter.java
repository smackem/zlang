package net.smackem.zlang.emit.bytecode;

import net.smackem.zlang.emit.ir.Instruction;
import net.smackem.zlang.emit.ir.Program;
import net.smackem.zlang.symbols.FunctionSymbol;
import net.smackem.zlang.symbols.InterfaceSymbol;
import net.smackem.zlang.symbols.StructSymbol;
import net.smackem.zlang.symbols.Type;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;

public class ByteCodeWriter implements AutoCloseable {
    public static final byte MAJOR_VERSION = 0;
    public static final byte MINOR_VERSION = 1;

    private final ConstSegmentWriter constSegment = new ConstSegmentWriter();

    /**
     * ZL byte code format v0.1:
     * - header (20 bytes)
     * - const segment (length @header)
     * - global segment (length @header)
     * - code segment (all remaining bytes)
     */
    public void writeProgram(Program program, OutputStream os) throws Exception {
        // render const segment to memory
        renderTypes(program.types());
        renderFunctions(program.codeMap().keySet());
        // render code segment to memory
        final byte[] codeSegment = renderCode(program.instructions());
        final byte[] constSegment = this.constSegment.fixup();

        // header
        writeHeaderBytes(os);
        // const segment
        os.write(constSegment);
        // global segment - chunk of zeroed memory
        os.write(new byte[program.globalSegmentSize()]);
        // code
        os.write(codeSegment);
    }

    private void writeHeaderBytes(OutputStream os) throws IOException {
        final byte[] zeroIntegerBytes = new byte[4];
        os.write((byte) 'Z');
        os.write((byte) 'L');
        os.write(MAJOR_VERSION);
        os.write(MINOR_VERSION);
        // entry point base pc @ offset 4
        os.write(zeroIntegerBytes);
        // entry point pc @ offset 8
        os.write(zeroIntegerBytes);
        // const segment size @ offset 12
        os.write(zeroIntegerBytes);
        // global segment size @ offset 16
        os.write(zeroIntegerBytes);
        // total header size = 20
    }

    private void renderTypes(Collection<Type> types) throws IOException {
        // write interfaces first
        for (final Type type : types) {
            if (type instanceof InterfaceSymbol ifs) {
                constSegment.writeType(ifs);
            }
        }
        // ... then structs
        for (final Type type : types) {
            if (type instanceof StructSymbol struct) {
                constSegment.writeType(struct);
            }
        }
    }

    private void renderFunctions(Collection<FunctionSymbol> functions) throws IOException {
        for (final FunctionSymbol function : functions) {
            this.constSegment.writeFunction(function);
        }
    }

    private byte[] renderCode(Collection<Instruction> instructions) throws Exception {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (final NativeValueWriter writer = new NativeValueWriter(bos)) {
            for (final Instruction instr : instructions) {
                renderInstruction(instr, writer);
            }
        }
        return bos.toByteArray();
    }

    private void renderInstruction(Instruction instr, NativeValueWriter writer) throws IOException {
        instr.setAddress(writer.bytesWritten());
    }

    @Override
    public void close() throws Exception {
        this.constSegment.close();
    }
}
