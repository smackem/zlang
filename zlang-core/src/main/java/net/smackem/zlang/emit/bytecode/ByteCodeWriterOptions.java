package net.smackem.zlang.emit.bytecode;

public class ByteCodeWriterOptions {
    private boolean memoryImage = false;
    private int heapSize = 256 * 1024;
    private boolean limitHeapSize = false;
    private int maxStackDepth = 64;

    /**
     * @return {@code true} if a full in-memory should be created - including
     *      memory for the global segment, registers and the heap.
     *      In this case, a <c>direct</c> {@code ByteBuffer} is created.
     */
    public boolean isMemoryImage() {
        return this.memoryImage;
    }

    public ByteCodeWriterOptions isMemoryImage(boolean value) {
        this.memoryImage = value;
        return this;
    }

    /**
     * @return the minimum size of the heap or 0 to let the vm choose the heap size.
     */
    public int heapSize() {
        return this.heapSize;
    }

    public ByteCodeWriterOptions heapSize(int value) {
        this.heapSize = value;
        return this;
    }

    /**
     * @return {@code true} if there is a hard limit on the heap size (see {@link #heapSize()}).
     *      Otherwise, {@link #heapSize()} specifies the minimum heap size, which may be exceeded
     *      for alignment reason.
     */
    public boolean hasHeapSizeLimit() {
        return this.limitHeapSize;
    }

    public ByteCodeWriterOptions hasHeapSizeLimit(boolean value) {
        this.limitHeapSize = value;
        return this;
    }

    /**
     * @return the maximum stack depth for program execution. The default is 64.
     */
    public int maxStackDepth() {
        return this.maxStackDepth;
    }

    public ByteCodeWriterOptions maxStackDepth(int value) {
        this.maxStackDepth = value;
        return this;
    }

    @Override
    public String toString() {
        return "ByteCodeWriterOptions{" +
               "memoryImage=" + memoryImage +
               ", heapSize=" + heapSize +
               ", limitHeapSize=" + limitHeapSize +
               ", maxStackDepth=" + maxStackDepth +
               '}';
    }
}
