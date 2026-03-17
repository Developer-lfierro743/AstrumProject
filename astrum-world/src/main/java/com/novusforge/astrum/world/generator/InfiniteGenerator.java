package com.novusforge.astrum.world.generator;

import io.github.auburn.FastNoiseLite;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Infinite world generator using FastNoiseLite.
 * Generates chunks on background threads using Java Virtual Threads.
 */
public class InfiniteGenerator {
    
    private final FastNoiseLite noise;
    private final long seed;
    private final ExecutorService chunkGenerator;
    private final ConcurrentHashMap<Long, ChunkData> chunkCache = new ConcurrentHashMap<>();
    
    private static final int CHUNK_SIZE = 32;
    private static final int CHUNK_HEIGHT = 256;
    private static final int MAX_LOAD_DISTANCE = 8;
    
    public static final byte BLOCK_AIR = 0;
    public static final byte BLOCK_STONE = 1;
    public static final byte BLOCK_DIRT = 2;
    public static final byte BLOCK_GRASS = 3;
    public static final byte BLOCK_WATER = 4;
    public static final byte BLOCK_SAND = 5;
    public static final byte BLOCK_GRAVEL = 6;
    
    public static final byte BLOCK_FERROUS = 7;
    public static final byte BLOCK_AURUM = 8;
    public static final byte BLOCK_CUPRUM = 9;
    public static final byte BLOCK_ARGENTUM = 10;
    public static final byte BLOCK_SILICIUM = 11;
    
    public InfiniteGenerator(long seed) {
        this.seed = seed;
        this.noise = new FastNoiseLite();
        this.noise.SetSeed((int) seed);
        this.noise.SetFrequency(0.02f);
        this.noise.SetNoiseType(FastNoiseLite.NoiseType.OpenSimplex2);
        this.noise.SetFractalType(FastNoiseLite.FractalType.FBm);
        this.noise.SetFractalOctaves(6);
        this.noise.SetFractalGain(0.5f);
        this.noise.SetFractalLacunarity(2.0f);
        
        this.chunkGenerator = Executors.newVirtualThreadPerTaskExecutor();
    }
    
    public void generateChunkAsync(int chunkX, int chunkZ, ChunkCallback callback) {
        chunkGenerator.submit(() -> {
            ChunkData chunk = generateChunk(chunkX, chunkZ);
            chunkCache.put(getChunkKey(chunkX, chunkZ), chunk);
            callback.onChunkReady(chunkX, chunkZ, chunk);
        });
    }
    
    public ChunkData generateChunk(int chunkX, int chunkZ) {
        ChunkData chunk = new ChunkData(chunkX, chunkZ, CHUNK_SIZE, CHUNK_HEIGHT);
        
        for (int x = 0; x < CHUNK_SIZE; x++) {
            for (int z = 0; z < CHUNK_SIZE; z++) {
                int worldX = chunkX * CHUNK_SIZE + x;
                int worldZ = chunkZ * CHUNK_SIZE + z;
                
                float heightNoise = noise.GetNoise(worldX, worldZ);
                int surfaceHeight = (int) ((heightNoise + 1) * 0.5f * 64) + 32;
                
                float temperature = noise.GetNoise(worldX * 0.5f, worldZ * 0.5f + 1000);
                float humidity = noise.GetNoise(worldX * 0.5f + 2000, worldZ * 0.5f);
                
                generateColumn(chunk, x, z, surfaceHeight, temperature, humidity);
            }
        }
        
        return chunk;
    }
    
    private void generateColumn(ChunkData chunk, int localX, int localZ, int surfaceHeight, float temperature, float humidity) {
        for (int y = 0; y < CHUNK_HEIGHT; y++) {
            byte blockId = BLOCK_AIR;
            
            if (y < surfaceHeight - 4) {
                blockId = getOreBlock(y, surfaceHeight);
            } else if (y < surfaceHeight - 1) {
                blockId = BLOCK_STONE;
            } else if (y < surfaceHeight) {
                blockId = getSurfaceBlock(temperature, humidity, surfaceHeight);
            } else if (y < 32) {
                blockId = BLOCK_WATER;
            }
            
            chunk.setBlock(localX, y, localZ, blockId);
        }
    }
    
    private byte getSurfaceBlock(float temperature, float humidity, int surfaceHeight) {
        if (surfaceHeight < 30) {
            return BLOCK_SAND;
        } else if (surfaceHeight < 35) {
            return BLOCK_GRAVEL;
        } else if (temperature > 0.3f) {
            return BLOCK_GRASS;
        } else if (temperature < -0.3f) {
            return BLOCK_SAND;
        } else {
            return BLOCK_GRASS;
        }
    }
    
    private byte getOreBlock(int y, int surfaceHeight) {
        float oreNoise = noise.GetNoise(y * 0.1f, surfaceHeight * 0.1f);
        
        if (oreNoise > 0.7f) {
            return BLOCK_ARGENTUM;
        } else if (oreNoise > 0.6f) {
            return BLOCK_AURUM;
        } else if (oreNoise > 0.5f) {
            return BLOCK_CUPRUM;
        } else if (oreNoise > 0.4f) {
            return BLOCK_FERROUS;
        } else if (oreNoise > 0.3f) {
            return BLOCK_SILICIUM;
        }
        
        return BLOCK_STONE;
    }
    
    public ChunkData getChunk(int chunkX, int chunkZ) {
        return chunkCache.get(getChunkKey(chunkX, chunkZ));
    }
    
    public void unloadChunk(int chunkX, int chunkZ) {
        chunkCache.remove(getChunkKey(chunkX, chunkZ));
    }
    
    private long getChunkKey(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
    }
    
    public void shutdown() {
        chunkGenerator.shutdown();
        chunkCache.clear();
    }
    
    public long getSeed() {
        return seed;
    }
    
    public interface ChunkCallback {
        void onChunkReady(int chunkX, int chunkZ, ChunkData chunk);
    }
    
    public static class ChunkData {
        private final int chunkX;
        private final int chunkZ;
        private final int sizeX;
        private final int sizeY;
        private final byte[] data;
        
        public ChunkData(int chunkX, int chunkZ, int sizeX, int sizeY) {
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.sizeX = sizeX;
            this.sizeY = sizeY;
            this.data = new byte[sizeX * sizeY * sizeX];
        }
        
        public void setBlock(int x, int y, int z, byte blockId) {
            if (x >= 0 && x < sizeX && y >= 0 && y < sizeY && z >= 0 && z < sizeX) {
                data[(y * sizeX + z) * sizeX + x] = blockId;
            }
        }
        
        public byte getBlock(int x, int y, int z) {
            if (x >= 0 && x < sizeX && y >= 0 && y < sizeY && z >= 0 && z < sizeX) {
                return data[(y * sizeX + z) * sizeX + x];
            }
            return 0;
        }
        
        public int getChunkX() { return chunkX; }
        public int getChunkZ() { return chunkZ; }
        public int getSizeX() { return sizeX; }
        public int getSizeY() { return sizeY; }
        public byte[] getData() { return data; }
    }
}
