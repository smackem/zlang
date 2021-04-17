package net.smackem.zlang.interpret;

import net.smackem.zlang.emit.ir.Program;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

public class Interpreter {
    private static final Logger log = LoggerFactory.getLogger(Interpreter.class);

    public static void run(ByteBuffer zl, Program program) {
        final byte[] heap = Zln.executeProgram(zl);
        assert heap != null;
        log.info("returned heap size: {}", heap.length);
    }
}
