package net.smackem.zlang.emit.bytecode;

import net.smackem.zlang.symbols.BuiltInTypeSymbol;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

class NativeValueWriter implements AutoCloseable {

    private final OutputStream os;
    private final ByteBuffer buf = ByteBuffer.allocate(1024).order(ByteOrder.nativeOrder());
    private int bytesFlushed;

    public NativeValueWriter(OutputStream os) {
        this.os = Objects.requireNonNull(os);
    }

    public int bytesWritten() {
        return this.bytesFlushed + this.buf.position();
    }

    OutputStream outputStream() {
        return this.os;
    }

    public void writeByte(byte value) throws IOException {
        if (this.buf.hasRemaining() == false) {
            flush();
        }
        this.buf.put(value);
    }

    public void writeByte(int value) throws IOException {
        if (this.buf.hasRemaining() == false) {
            flush();
        }
        this.buf.put((byte) value);
    }

    public void writeInt32(int value) throws IOException {
        if (this.buf.remaining() < BuiltInTypeSymbol.INT.byteSize()) {
            flush();
        }
        this.buf.putInt(value);
    }

    public void writeInt64(long value) throws IOException {
        if (this.buf.remaining() < 8) {
            flush();
        }
        this.buf.putLong(value);
    }

    public void writeAddr(long value) throws IOException {
        if (this.buf.remaining() < BuiltInTypeSymbol.OBJECT.byteSize()) {
            flush();
        }
        this.buf.putInt((int) value);
    }

    public void writeFloat64(double value) throws IOException {
        if (this.buf.remaining() < BuiltInTypeSymbol.FLOAT.byteSize()) {
            flush();
        }
        this.buf.putDouble(value);
    }

    public void writeString(String value) throws IOException {
        final byte[] bytes = value.getBytes(StandardCharsets.US_ASCII);
        writeZeroTerminated(bytes);
    }

    /**
     * Writes the given string, padding it if necessary with zeroes to match {@code targetLength}.
     */
    public void writeString(String value, int targetLength) throws IOException {
        final byte[] bytes = value.getBytes(StandardCharsets.US_ASCII);
        if (bytes.length > targetLength - 1) {
            throw new IllegalArgumentException("string is longer than length allows");
        }
        writeZeroTerminated(bytes);
        for (int length = bytes.length + 1; length < targetLength; length++) {
            writeByte((byte) 0);
        }
    }

    @Override
    public void close() throws Exception {
        flush();
        this.os.close();
    }

    private void writeZeroTerminated(byte[] bytes) throws IOException {
        final int length = bytes.length + 1; // +1 for terminating zero
        if (this.buf.remaining() < length) {
            flush();
        }
        if (this.buf.remaining() < length) {
            // string is too big for buffer - write it directly to output stream
            this.os.write(bytes);
            this.os.write(0);
            this.bytesFlushed += length;
            return;
        }
        this.buf.put(bytes);
        this.buf.put((byte) 0);
    }

    void flush() throws IOException {
        int size = this.buf.position();
        if (size > 0) {
            this.os.write(this.buf.array(), this.buf.arrayOffset(), size);
            this.buf.clear();
            this.bytesFlushed += size;
        }
    }
}
