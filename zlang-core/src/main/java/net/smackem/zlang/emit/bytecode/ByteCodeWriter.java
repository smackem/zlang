package net.smackem.zlang.emit.bytecode;

import net.smackem.zlang.emit.ir.Instruction;
import net.smackem.zlang.symbols.Type;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;

public class ByteCodeWriter implements AutoCloseable {

    private final ConstSegmentWriter constSegment = new ConstSegmentWriter();
    private final NativeValueWriter codeSegment = new NativeValueWriter(new ByteArrayOutputStream());

    private void writeTypes(Collection<Type> types) {
    }

    private void writeInstruction(Instruction instr) throws IOException {
    }

    @Override
    public void close() throws Exception {
        this.constSegment.close();
        this.codeSegment.close();
    }
}
