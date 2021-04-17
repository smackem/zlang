package net.smackem.zlang.interpret;

import java.nio.ByteBuffer;

class Zln {
    private Zln() { }

    public static native byte[] executeProgram(ByteBuffer zl);
}
