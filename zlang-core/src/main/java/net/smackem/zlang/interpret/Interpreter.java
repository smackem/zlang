package net.smackem.zlang.interpret;

import net.smackem.zlang.emit.ir.Program;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

public class Interpreter {
    private static final Logger log = LoggerFactory.getLogger(Interpreter.class);

    private Interpreter() { }

    public static int run(ByteBuffer zl, Program program) {
        final int retVal = Zln.executeProgram(zl);
        log.info("returned value: {}", retVal);
        return retVal;
    }
}
