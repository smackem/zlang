package net.smackem.zlang.emit.bytecode;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

class ChunkWriter extends NativeValueWriter {

    private int mark;

    public ChunkWriter() {
        super(new ByteArrayOutputStream());
    }

    public ByteBuffer toByteBuffer() {
        final byte[] bytes = ((ByteArrayOutputStream) this.outputStream()).toByteArray();
        return ByteBuffer.wrap(bytes).order(ByteOrder.nativeOrder());
    }

    public void setMark() {
        this.mark = this.bytesWritten();
    }

    public int bytesWrittenSinceMark() {
        return this.bytesWritten() - this.mark;
    }
}
