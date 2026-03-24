package com.novusforge.astrum.engine;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * CubeMesh - Utility for generating cube vertex data
 * 6 faces × 2 triangles × 3 vertices = 36 vertices total
 * Vertex format: position (x,y,z) + color (r,g,b) = 6 floats
 */
public class CubeMesh {

    public static final int VERTEX_SIZE = 6;
    public static final int TOTAL_VERTICES = 36;

    /**
     * Generate textured cube with face shading (Minecraft-style)
     */
    public static float[] generateTexturedCube(float x, float y, float z, float r, float g, float b) {
        float[] vertices = new float[TOTAL_VERTICES * VERTEX_SIZE];
        int idx = 0;

        float topBright = 1.0f;
        float sideBright = 0.8f;
        float bottomBright = 0.6f;

        // FRONT face (+Z)
        addFace(vertices, idx, x, y, z + 1, x + 1, y, z + 1, x + 1, y + 1, z + 1, x, y + 1, z + 1,
            r * sideBright, g * sideBright, b * sideBright);
        idx += 24;

        // BACK face (-Z)
        addFace(vertices, idx, x + 1, y, z, x, y, z, x, y + 1, z, x + 1, y + 1, z,
            r * sideBright, g * sideBright, b * sideBright);
        idx += 24;

        // TOP face (+Y)
        addFace(vertices, idx, x, y + 1, z + 1, x + 1, y + 1, z + 1, x + 1, y + 1, z, x, y + 1, z,
            r * topBright, g * topBright, b * topBright);
        idx += 24;

        // BOTTOM face (-Y)
        addFace(vertices, idx, x, y, z, x + 1, y, z, x + 1, y, z + 1, x, y, z + 1,
            r * bottomBright, g * bottomBright, b * bottomBright);
        idx += 24;

        // LEFT face (-X)
        addFace(vertices, idx, x, y, z, x, y, z + 1, x, y + 1, z + 1, x, y + 1, z,
            r * sideBright, g * sideBright, b * sideBright);
        idx += 24;

        // RIGHT face (+X)
        addFace(vertices, idx, x + 1, y, z + 1, x + 1, y, z, x + 1, y + 1, z, x + 1, y + 1, z + 1,
            r * sideBright, g * sideBright, b * sideBright);

        return vertices;
    }

    private static void addFace(float[] vertices, int startIdx,
                                float x0, float y0, float z0,
                                float x1, float y1, float z1,
                                float x2, float y2, float z2,
                                float x3, float y3, float z3,
                                float r, float g, float b) {
        int idx = startIdx;

        // Triangle 1
        vertices[idx++] = x0; vertices[idx++] = y0; vertices[idx++] = z0;
        vertices[idx++] = r;  vertices[idx++] = g;  vertices[idx++] = b;

        vertices[idx++] = x1; vertices[idx++] = y1; vertices[idx++] = z1;
        vertices[idx++] = r;  vertices[idx++] = g;  vertices[idx++] = b;

        vertices[idx++] = x2; vertices[idx++] = y2; vertices[idx++] = z2;
        vertices[idx++] = r;  vertices[idx++] = g;  vertices[idx++] = b;

        // Triangle 2
        vertices[idx++] = x0; vertices[idx++] = y0; vertices[idx++] = z0;
        vertices[idx++] = r;  vertices[idx++] = g;  vertices[idx++] = b;

        vertices[idx++] = x2; vertices[idx++] = y2; vertices[idx++] = z2;
        vertices[idx++] = r;  vertices[idx++] = g;  vertices[idx++] = b;

        vertices[idx++] = x3; vertices[idx++] = y3; vertices[idx++] = z3;
        vertices[idx++] = r;  vertices[idx++] = g;  vertices[idx++] = b;
    }
}
