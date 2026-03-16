package com.novusforge.astrum.common.memory;

import org.junit.jupiter.api.Test;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for NativeMemory to verify off-heap safety and Panama API interop.
 */
public class NativeMemoryTest {

    @Test
    public void testAllocation() {
        try (NativeMemory memory = new NativeMemory()) {
            MemorySegment segment = memory.allocate(1024);
            assertEquals(1024, segment.byteSize(), "Allocated size should match requested size.");
        }
    }

    @Test
    public void testIntArrayAllocation() {
        try (NativeMemory memory = new NativeMemory()) {
            int[] data = {1, 2, 3, 4, 5};
            MemorySegment segment = memory.allocateInts(data);
            
            assertEquals(data.length * 4, segment.byteSize(), "Allocated size should match array size in bytes.");
            
            for (int i = 0; i < data.length; i++) {
                int val = segment.getAtIndex(ValueLayout.JAVA_INT, i);
                assertEquals(data[i], val, "Value at index " + i + " should match source array.");
            }
        }
    }

    @Test
    public void testAutoClosure() {
        MemorySegment leakCheck;
        try (NativeMemory memory = new NativeMemory()) {
            leakCheck = memory.allocate(64);
            assertTrue(leakCheck.isAlive(), "Memory should be alive within the try-with-resources block.");
        }
        assertFalse(leakCheck.isAlive(), "Memory should be closed/deallocated after the block.");
    }
}
