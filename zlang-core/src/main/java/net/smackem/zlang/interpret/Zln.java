package net.smackem.zlang.interpret;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

// -Djava.library.path=$ProjectFileDir$/zln/cmake-build-debug
class Zln {
    private static final Logger log = LoggerFactory.getLogger(Zln.class);

    private Zln() { }

    static {
        try {
            System.loadLibrary("zln");
            log.info("zln library available");
        } catch (UnsatisfiedLinkError e) {
            log.error("zln not available", e);
        }
    }

    public static native int executeProgram(ByteBuffer zl);
}
