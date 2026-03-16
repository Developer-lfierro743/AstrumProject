package com.novusforge.astrum.common.memory;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

/**
 * High-performance off-heap memory management using the Java 21+ Panama API (FFM).
 * Designed for zero-copy interop with Vulkan and native libraries.
 */
public class NativeMemory implements AutoCloseable {
    @SuppressWarnings("preview")
    private final Arena arena;

    @SuppressWarnings("preview")
    public NativeMemory() {
        // Use a confined arena for thread-safe, deterministic deallocation
        this.arena = Arena.ofConfined();
    }

    /**
     * Allocates a block of off-heap memory.
     */
    @SuppressWarnings("preview")
    public MemorySegment allocate(long bytes) {
        return arena.allocate(bytes);
    }

    /**
     * Allocates an array of integers off-heap.
     */
    @SuppressWarnings("preview")
    public MemorySegment allocateInts(int[] data) {
        @SuppressWarnings("preview")
        MemorySegment segment = arena.allocate(data.length * ValueLayout.JAVA_INT.byteSize(), ValueLayout.JAVA_INT.byteAlignment());
        segment.copyFrom(MemorySegment.ofArray(data));
        return segment;
    }

    /**
     * Allocates a block of memory for a specific value layout.
     */
    @SuppressWarnings("preview")
    public MemorySegment allocate(@SuppressWarnings("preview") ValueLayout layout) {
        return arena.allocate(layout);
    }

    @Override
    public void close() {
        arena.close();
    }
}
