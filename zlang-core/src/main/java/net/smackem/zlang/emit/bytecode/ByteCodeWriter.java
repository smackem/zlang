package net.smackem.zlang.emit.bytecode;

import net.smackem.zlang.emit.ir.*;
import net.smackem.zlang.symbols.FunctionSymbol;
import net.smackem.zlang.symbols.InterfaceSymbol;
import net.smackem.zlang.symbols.StructSymbol;
import net.smackem.zlang.symbols.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collection;

public class ByteCodeWriter implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(ByteCodeWriter.class);
    public static final byte MAJOR_VERSION = 0;
    public static final byte MINOR_VERSION = 1;

    private final ConstSegmentWriter constSegment = new ConstSegmentWriter();

    /**
     * ZL byte code format v0.1:
     * 1) header (20 bytes)
     * 2) code segment (length @header)
     * 3) const segment (length @header)
     * 4) global segment (zeroed memory, length @header)
     * 5) heap memory (zeroed memory, all remaining bytes)
     */
    public ByteBuffer writeProgram(Program program, int heapSize) throws Exception {
        // render const segment to memory
        renderTypes(program.types());
        renderFunctions(program.codeMap().keySet());
        // render code segment to memory
        final byte[] codeSegment = renderCode(program.instructions(), program.labels());
        final byte[] constSegment = this.constSegment.fixup(program.codeMap());
        final int headerSize = 20;
        final int globalSegmentSize = program.globalSegmentSize();
        final int size = headerSize + codeSegment.length + constSegment.length + globalSegmentSize + heapSize;
        final ByteBuffer buf = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder());
        // 1) header
        writeHeaderBytes(buf, codeSegment.length, constSegment.length, globalSegmentSize, program.entryPoint());
        // 2) code segment
        buf.put(headerSize, codeSegment);
        // 3) const segment
        buf.put(headerSize + codeSegment.length, constSegment);
        // 4) global segment - just reserve space
        // 5) heap segment - just reserve space
        return buf;
    }

    private void writeHeaderBytes(ByteBuffer buf,
                                  int codeSegmentSize,
                                  int constSegmentSize,
                                  int globalSegmentSize,
                                  FunctionSymbol entryPoint) throws IOException {
        log.info("ZL header: codeSize={} constSize={} globalSize={} entryPoint={}",
                codeSegmentSize, constSegmentSize, globalSegmentSize, entryPoint.address());
        buf.put(0, (byte) 'Z');
        buf.put(1, (byte) 'L');
        buf.put(2, MAJOR_VERSION);
        buf.put(3, MINOR_VERSION);
        buf.putInt(4, codeSegmentSize);
        buf.putInt(8, constSegmentSize);
        buf.putInt(12, globalSegmentSize);
        buf.putInt(16, entryPoint.address());
        // max register count
        // max stack depth
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
        int highestRegister = -1;
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (final NativeValueWriter writer = new NativeValueWriter(bos)) {
            for (final Instruction instr : instructions) {
                highestRegister = Math.max(highestRegister, getHighestRegister(instr));
                renderInstruction(instr, writer);
            }
        }
        log.info("highest register = {}", highestRegister);

        // fixup branch instructions using labels
        final byte[] bytes = bos.toByteArray();
        final ByteBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.nativeOrder());
        for (final Label label : labels) {
            for (final Instruction sourceInstr : label.sources()) {
                final int offset = sourceInstr.address();
                switch (sourceInstr.opCode()) {
                    case Br -> buf.putInt(offset + 1, label.target().address());
                    case Br_zero -> buf.putInt(offset + 2, label.target().address());
                    default -> {
                        log.error("invalid opcode for branch source instruction: {}", sourceInstr);
                        assert false;
                    }
                }
            }
        }
        return bytes;
    }

    private static int getHighestRegister(Instruction instr) {
        int registerNumber = -1;
        for (int i = 0; i < 3; i++) {
            final Register r = instr.registerArg(i);
            if (r == null) {
                continue;
            }
            if (r.number() > registerNumber) {
                registerNumber = r.number();
            }
        }
        return registerNumber;
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
    }
}
