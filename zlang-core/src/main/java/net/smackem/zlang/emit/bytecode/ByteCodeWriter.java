package net.smackem.zlang.emit.bytecode;

import net.smackem.zlang.emit.ir.FunctionCode;
import net.smackem.zlang.emit.ir.Instruction;
import net.smackem.zlang.emit.ir.Label;
import net.smackem.zlang.emit.ir.Program;
import net.smackem.zlang.symbols.FunctionSymbol;
import net.smackem.zlang.symbols.InterfaceSymbol;
import net.smackem.zlang.symbols.StructSymbol;
import net.smackem.zlang.symbols.Type;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collection;
import java.util.Objects;

public class ByteCodeWriter implements AutoCloseable {
    public static final byte MAJOR_VERSION = 0;
    public static final byte MINOR_VERSION = 1;

    private final OutputStream os;
    private final ConstSegmentWriter constSegment = new ConstSegmentWriter();

    public ByteCodeWriter(OutputStream os) {
        this.os = Objects.requireNonNull(os);
    }

    /**
     * ZL byte code format v0.1:
     * - header (20 bytes)
     * - const segment (length @header)
     * - global segment (length @header)
     * - code segment (all remaining bytes)
     */
    public void writeProgram(Program program) throws Exception {
        // render const segment to memory
        renderTypes(program.types());
        renderFunctions(program.codeMap().keySet());
        // render code segment to memory
        final byte[] codeSegment = renderCode(program.instructions(), program.labels());
        final byte[] constSegment = this.constSegment.fixup(program.codeMap());

        // header
        writeHeaderBytes(program, constSegment.length);
        // const segment
        this.os.write(constSegment);
        // global segment - chunk of zeroed memory
        this.os.write(new byte[program.globalSegmentSize()]);
        // code
        this.os.write(codeSegment);
    }

    private void writeHeaderBytes(Program program, int constSegmentSize) throws IOException {
        final ByteBuffer buf = ByteBuffer.allocate(20).order(ByteOrder.nativeOrder());
        buf.put(0, (byte) 'Z');
        buf.put(1, (byte) 'L');
        buf.put(2, MAJOR_VERSION);
        buf.put(3, MINOR_VERSION);
        final FunctionCode entryPoint = program.codeMap().get(program.entryPoint());
        buf.putInt(4, entryPoint.moduleInstr().address());
        buf.putInt(8, entryPoint.firstInstr().address());
        buf.putInt(12, constSegmentSize);
        buf.putInt(16, program.globalSegmentSize());
        this.os.write(buf.array(), buf.arrayOffset(), buf.capacity());
    }

    private void renderTypes(Collection<Type> types) throws IOException {
        // write interfaces first
        for (final Type type : types) {
            if (type instanceof InterfaceSymbol ifs) {
                this.constSegment.writeType(ifs);
            }
        }
        // ... then structs
        for (final Type type : types) {
            if (type instanceof StructSymbol struct) {
                this.constSegment.writeType(struct);
            }
        }
    }

    private void renderFunctions(Collection<FunctionSymbol> functions) throws IOException {
        for (final FunctionSymbol function : functions) {
            this.constSegment.writeFunction(function);
        }
    }

    private byte[] renderCode(Collection<Instruction> instructions, Collection<Label> labels) throws Exception {
        // write instructions
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (final NativeValueWriter writer = new NativeValueWriter(bos)) {
            for (final Instruction instr : instructions) {
                renderInstruction(instr, writer);
            }
        }
        // fixup branch instructions using labels
        final byte[] bytes = bos.toByteArray();
        final ByteBuffer buf = ByteBuffer.wrap(bytes);
        for (final Label label : labels) {
            for (final Instruction sourceInstr : label.sources()) {
                final int offset = sourceInstr.address();
                switch (sourceInstr.opCode()) {
                    case Br -> buf.putInt(offset, label.target().address());
                    case Br_zero -> buf.putInt(offset + 1, label.target().address());
                    default -> {
                        assert false;
                    }
                }
            }
        }
        return bytes;
    }

    private void renderInstruction(Instruction instr, NativeValueWriter writer) throws IOException {
        instr.setAddress(writer.bytesWritten());
        writer.writeByte(instr.opCode().code());
        switch (instr.opCode()) {
            case Nop, Ret, Halt -> { }
            case LdGlb_i32, LdGlb_f64, LdGlb_u8, LdGlb_ref, LdGlb_ptr,
                    StGlb_i32, StGlb_f64, StGlb_u8, StGlb_ref, StGlb_ptr,
                    Ldc_i32, Ldc_ref -> {
                writer.writeByte(instr.registerArg(0).number());
                writer.writeAddr(instr.intArg());
            }
            case LdFld_i32, LdFld_f64, LdFld_u8, LdFld_ref, LdFld_ptr,
                    StFld_i32, StFld_f64, StFld_u8, StFld_ref, StFld_ptr -> {
                writer.writeByte(instr.registerArg(0).number());
                writer.writeByte(instr.registerArg(1).number());
                writer.writeAddr(instr.intArg());
            }
            case LdElem_i32, LdElem_f64, LdElem_u8, LdElem_ref, LdElem_ptr,
                    StElem_i32, StElem_f64, StElem_u8, StElem_ref, StElem_ptr,
                    Add_i32, Add_f64, Add_u8, Add_str,
                    Sub_i32, Sub_f64, Sub_u8,
                    Mul_i32, Mul_f64, Mul_u8,
                    Div_i32, Div_f64, Div_u8,
                    Eq_i32, Eq_f64, Eq_u8, Eq_str, Eq_ref, Eq_ptr,
                    Ne_i32, Ne_f64, Ne_u8, Ne_str, Ne_ref, Ne_ptr,
                    Gt_i32, Gt_f64, Gt_u8, Gt_str,
                    Ge_i32, Ge_f64, Ge_u8, Ge_str,
                    Lt_i32, Lt_f64, Lt_u8, Lt_str,
                    Le_i32, Le_f64, Le_u8, Le_str,
                    And, Or -> {
                writer.writeByte(instr.registerArg(0).number());
                writer.writeByte(instr.registerArg(1).number());
                writer.writeByte(instr.registerArg(2).number());
            }
            case Ldc_f64 -> {
                final int addr = this.constSegment.bytesWritten();
                this.constSegment.writeFloat64(instr.floatArg());
                writer.writeByte(instr.registerArg(0).number());
                writer.writeAddr(addr);
            }
            case Ldc_zero, AddRef, RemoveRef ->
                writer.writeByte(instr.registerArg(0).number());
            case NewArr_i32, NewArr_f64, NewArr_u8, NewArr_ref, NewArr_ptr,
                    Mov -> {
                writer.writeByte(instr.registerArg(0).number());
                writer.writeByte(instr.registerArg(1).number());
            }
            case Br_zero -> {
                assert instr.labelArg() != null;
                writer.writeByte(instr.registerArg(0).number());
                writer.writeAddr(0); // needs fixup
            }
            case Br -> writer.writeAddr(0); // needs fixup
            case Call -> {
                writer.writeByte(instr.registerArg(0).number());
                writer.writeByte(instr.registerArg(1).number());
                writer.writeAddr(instr.symbolArg().address());
            }
            case Conv_i32, Conv_f64, Conv_u8, Conv_str, Conv_ref, Conv_ptr -> {
                writer.writeByte(instr.registerArg(0).number());
                writer.writeByte(instr.registerArg(1).number());
                writer.writeInt32((int) instr.intArg());
            }
            case NewObj -> {
                writer.writeByte(instr.registerArg(0).number());
                writer.writeAddr(instr.symbolArg().address());
            }
            case NewStr -> {
                assert false;
            }
        }
    }

    @Override
    public void close() throws Exception {
        this.constSegment.close();
        this.os.close();
    }
}
