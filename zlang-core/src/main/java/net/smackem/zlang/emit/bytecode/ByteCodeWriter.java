package net.smackem.zlang.emit.bytecode;

import net.smackem.zlang.emit.ir.*;
import net.smackem.zlang.symbols.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collection;

public class ByteCodeWriter implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(ByteCodeWriter.class);
    private static final int[] bufferSizes;

    static {
        final int K = 1024;
        final int M = K * K;
        bufferSizes = new int[] {
                16 * K,
                32 * K,
                64 * K,
                256 * K,
                M,
                4 * M,
                16 * M,
                64 * M
        };
    }

    private final ConstSegmentWriter constSegment = new ConstSegmentWriter();

    /**
     * ZL byte code format v0.1:
     * 1) header (ByteCode.HEADER_SIZE bytes)
     * 2) code segment (length @header)
     * 3) const segment (length @header)
     * 4) global segment (zeroed memory, length @header)
     * 5) heap memory (zeroed memory, all remaining bytes)
     */
    public ByteBuffer writeProgram(Program program, int heapSize, int maxStackDepth) throws Exception {
        // render const segment to memory
        renderTypes(program.types());
        renderFunctions(program.codeMap().keySet());
        // render code segment to memory
        final RenderedCode code = renderCode(program.instructions(), program.labels());
        final byte[] constSegment = this.constSegment.fixup(program.types(), program.codeMap());
        final int headerSize = ByteCode.HEADER_SIZE;
        final int globalSegmentSize = program.globalSegmentSize();
        // approximate minimum buffer size
        final int approximateSize = headerSize
                + code.segment.length
                + constSegment.length
                + globalSegmentSize
                + code.registerCount * maxStackDepth * 8 // registers: assume each register has 8 bytes
                + maxStackDepth * 32 // approximate stack frame size
                + heapSize;
        final int size = getBufferSize(approximateSize);
        log.info("approximate required size: {} -> allocate buffer of size {}", approximateSize, size);
        final ByteBuffer buf = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder());
        // 1) header
        writeHeaderBytes(buf, code.segment.length, constSegment.length, globalSegmentSize, program.entryPoint(), code.registerCount, maxStackDepth);
        // 2) code segment
        buf.put(headerSize, code.segment);
        // 3) const segment
        buf.put(headerSize + code.segment.length, constSegment);
        // 4) global segment - just reserve space
        // 5) register segment - just reserve space
        // 6) stack frame segment - just reserve space
        // 5) heap segment - just reserve space
        return buf;
    }

    private static int getBufferSize(int approximateSize) {
        for (final int size : bufferSizes) {
            if (approximateSize < size) {
                return size;
            }
        }
        throw new IllegalArgumentException("approximateSize %d exceeds maximum buffer size".formatted(approximateSize));
    }

    private void writeHeaderBytes(ByteBuffer buf,
                                  int codeSegmentSize,
                                  int constSegmentSize,
                                  int globalSegmentSize,
                                  FunctionSymbol entryPoint,
                                  int registerCount,
                                  int maxStackDepth) {
        log.info("ZL header: codeSize={} constSize={} globalSize={} entryPoint={}",
                codeSegmentSize, constSegmentSize, globalSegmentSize, entryPoint.address());
        buf.put(0, (byte) 'Z');
        buf.put(1, (byte) 'L');
        buf.put(2, ByteCode.MAJOR_VERSION);
        buf.put(3, ByteCode.MINOR_VERSION);
        buf.putInt(4, codeSegmentSize);
        buf.putInt(8, constSegmentSize);
        buf.putInt(12, globalSegmentSize);
        buf.putInt(16, entryPoint.address());
        buf.putInt(20, registerCount);
        buf.putInt(24, maxStackDepth);
    }

    private void renderTypes(Collection<Type> types) throws IOException {
        // write interfaces first
        for (final Type type : types) {
            if (type instanceof InterfaceSymbol ifs) {
                this.constSegment.writeType(ifs);
            }
        }
        // ... then structs and lists
        for (final Type type : types) {
            if (type instanceof StructSymbol || type instanceof ListType) {
                this.constSegment.writeType((AggregateTypeSymbol) type);
            }
        }
    }

    private void renderFunctions(Collection<FunctionSymbol> functions) throws IOException {
        for (final FunctionSymbol function : functions) {
            this.constSegment.writeFunction(function);
        }
    }

    private static record RenderedCode(byte[] segment, int registerCount) { }

    private RenderedCode renderCode(Collection<Instruction> instructions, Collection<Label> labels) throws Exception {
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
        return new RenderedCode(bytes, highestRegister + 1);
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
                    Ldc_i32 -> {
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
            case Ldc_str -> {
                final int addr = this.constSegment.bytesWritten();
                this.constSegment.writeString(instr.strArg());
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
            case Call, CallVirt, Invoke -> {
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
            default -> throw new RuntimeException("unsupported opcode " + instr.opCode());
        }
    }

    @Override
    public void close() throws Exception {
        this.constSegment.close();
    }
}
