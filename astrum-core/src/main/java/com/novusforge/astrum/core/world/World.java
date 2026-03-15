package com.novusforge.astrum.core.world;

import com.novusforge.astrum.common.world.VoxelData;
import org.joml.Vector3i;
import java.util.HashMap;
import java.util.Map;

/**
 * The World container for Astrum.
 * Manages chunks and entities using a high-performance ECS architecture.
 */
public class World {
    private final Map<Vector3i, VoxelData> chunks = new HashMap<>();

    public void setBlock(int x, int y, int z, byte blockId) {
        Vector3i chunkPos = getChunkPosition(x, y, z);
        VoxelData chunk = chunks.computeIfAbsent(chunkPos, k -> new VoxelData());
        chunk.setBlock(getLocalCoord(x), getLocalCoord(y), getLocalCoord(z), blockId);
    }

    public byte getBlock(int x, int y, int z) {
        Vector3i chunkPos = getChunkPosition(x, y, z);
        VoxelData chunk = chunks.get(chunkPos);
        if (chunk == null) return 0;
        return chunk.getBlock(getLocalCoord(x), getLocalCoord(y), getLocalCoord(z));
    }

    private Vector3i getChunkPosition(int x, int y, int z) {
        return new Vector3i(
            Math.floorDiv(x, VoxelData.CHUNK_SIZE),
            Math.floorDiv(y, VoxelData.CHUNK_SIZE),
            Math.floorDiv(z, VoxelData.CHUNK_SIZE)
        );
    }

    private int getLocalCoord(int coord) {
        return Math.floorMod(coord, VoxelData.CHUNK_SIZE);
    }

    public Map<Vector3i, VoxelData> getChunks() {
        return chunks;
    }
}
