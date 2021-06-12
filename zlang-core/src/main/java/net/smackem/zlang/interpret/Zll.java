package net.smackem.zlang.interpret;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

// -Djava.library.path=$ProjectFileDir$/zln/cmake-build-debug
class Zll {
    private static final Logger log = LoggerFactory.getLogger(Zll.class);

    private Zll() { }

    static {
        try {
            System.loadLibrary("zll");
            log.info("zll library available");
        } catch (UnsatisfiedLinkError e) {
            log.error("zll not available", e);
        }
    }

    public static native int executeProgram(ByteBuffer zap);
}
