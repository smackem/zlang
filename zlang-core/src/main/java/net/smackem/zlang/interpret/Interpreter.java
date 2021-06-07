package net.smackem.zlang.interpret;

import net.smackem.zlang.emit.bytecode.ByteCodeReader;
import net.smackem.zlang.emit.ir.Program;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Map;

public class Interpreter {
    private static final Logger log = LoggerFactory.getLogger(Interpreter.class);

    private Interpreter() { }

    public static Map<String, Object> run(ByteBuffer zl, Program program) {
        final int heapOffset = run(zl);
        return ByteCodeReader.readGlobals(zl, heapOffset, program);
    }

    static int run(ByteBuffer zl) {
        return Zln.executeProgram(zl);
    }
}
