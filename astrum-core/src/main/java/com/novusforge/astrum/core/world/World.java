package com.novusforge.astrum.core.world;

import com.novusforge.astrum.common.world.ChunkMath;
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
        Vector3i chunkPos = ChunkMath.worldToChunk(x, y, z);
        VoxelData chunk = chunks.computeIfAbsent(chunkPos, k -> new VoxelData());
        chunk.setBlock(ChunkMath.toLocal(x), ChunkMath.toLocal(y), ChunkMath.toLocal(z), blockId);
    }

    public byte getBlock(int x, int y, int z) {
        Vector3i chunkPos = ChunkMath.worldToChunk(x, y, z);
        VoxelData chunk = chunks.get(chunkPos);
        if (chunk == null) return 0;
        return chunk.getBlock(ChunkMath.toLocal(x), ChunkMath.toLocal(y), ChunkMath.toLocal(z));
    }

    public Map<Vector3i, VoxelData> getChunks() {
        return chunks;
    }
}
