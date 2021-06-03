package net.smackem.zlang.emit.bytecode;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

class ChunkWriter extends NativeValueWriter {

    public ChunkWriter() {
        super(new ByteArrayOutputStream());
    }

    public ByteBuffer toByteBuffer() {
        final byte[] bytes = ((ByteArrayOutputStream) this.outputStream()).toByteArray();
        return ByteBuffer.wrap(bytes).order(ByteOrder.nativeOrder());
    }
}
