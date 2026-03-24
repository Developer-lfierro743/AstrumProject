package com.novusforge.astrum.world;

/**
 * FrustumCuller - Visibility testing
 * Formula: "Frustum culling(Bounding box of the chunk is within the camera point of view try to render)"
 */
public class FrustumCuller {
    
    public static final int LEFT = 0;
    public static final int RIGHT = 1;
    public static final int BOTTOM = 2;
    public static final int TOP = 3;
    public static final int NEAR = 4;
    public static final int FAR = 5;

    /**
     * Check if chunk is visible in view frustum
     * Returns true if frustum planes are null (no culling)
     */
    public static boolean isChunkVisible(int cx, int cy, int cz, float[] planes) {
        if (planes == null || planes.length < 24) return true;

        float minX = cx * Chunk.SIZE - 0.5f;
        float minY = cy * Chunk.SIZE - 0.5f;
        float minZ = cz * Chunk.SIZE - 0.5f;
        float maxX = minX + Chunk.SIZE + 0.5f;
        float maxY = minY + Chunk.SIZE + 0.5f;
        float maxZ = minZ + Chunk.SIZE + 0.5f;

        // Test against all 6 frustum planes
        for (int i = 0; i < 6; i++) {
            float nx = planes[i * 4];
            float ny = planes[i * 4 + 1];
            float nz = planes[i * 4 + 2];
            float d = planes[i * 4 + 3];

            // Get corner most in plane direction
            float px = (nx >= 0) ? maxX : minX;
            float py = (ny >= 0) ? maxY : minY;
            float pz = (nz >= 0) ? maxZ : minZ;

            if (nx * px + ny * py + nz * pz + d > 0) continue;

            // Get opposite corner
            px = (nx >= 0) ? minX : maxX;
            py = (ny >= 0) ? minY : maxY;
            pz = (nz >= 0) ? minZ : maxZ;

            if (nx * px + ny * py + nz * pz + d <= 0) {
                return false; // Outside frustum
            }
        }
        return true; // Inside or intersecting frustum
    }

    /**
     * Check if individual block is visible
     */
    public static boolean isBlockVisible(int wx, int wy, int wz, float[] planes) {
        if (planes == null || planes.length < 24) return true;

        float minX = wx - 0.5f;
        float minY = wy - 0.5f;
        float minZ = wz - 0.5f;
        float maxX = minX + 1.0f;
        float maxY = minY + 1.0f;
        float maxZ = minZ + 1.0f;

        for (int i = 0; i < 6; i++) {
            float nx = planes[i * 4];
            float ny = planes[i * 4 + 1];
            float nz = planes[i * 4 + 2];
            float d = planes[i * 4 + 3];

            float px = (nx >= 0) ? maxX : minX;
            float py = (ny >= 0) ? maxY : minY;
            float pz = (nz >= 0) ? maxZ : minZ;

            if (nx * px + ny * py + nz * pz + d > 0) continue;

            px = (nx >= 0) ? minX : maxX;
            py = (ny >= 0) ? minY : maxY;
            pz = (nz >= 0) ? minZ : maxZ;

            if (nx * px + ny * py + nz * pz + d <= 0) {
                return false;
            }
        }
        return true;
    }
}
