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
        final int retVal = Zln.executeProgram(zl);
        log.info("returned value: {}", retVal);
        return ByteCodeReader.readGlobals(zl, program);
    }
}
