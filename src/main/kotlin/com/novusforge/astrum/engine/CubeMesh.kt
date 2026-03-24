package com.novusforge.astrum.engine

/**
 * CubeMesh - Utility for generating cube vertex data
 * 6 faces × 2 triangles × 3 vertices = 36 vertices total
 * Vertex format: position (x,y,z) + color (r,g,b) = 6 floats
 * Kotlin conversion: Using object singleton and idiomatic math.
 */
object CubeMesh {

    const val VERTEX_SIZE = 6
    const val TOTAL_VERTICES = 36

    /**
     * Generate textured cube with face shading (Minecraft-style)
     */
    @JvmStatic
    fun generateTexturedCube(x: Float, y: Float, z: Float, r: Float, g: Float, b: Float): FloatArray {
        val vertices = FloatArray(TOTAL_VERTICES * VERTEX_SIZE)
        var idx = 0

        val topBright = 1.0f
        val sideBright = 0.8f
        val bottomBright = 0.6f

        // FRONT face (+Z)
        addFace(vertices, idx, x, y, z + 1, x + 1, y, z + 1, x + 1, y + 1, z + 1, x, y + 1, z + 1,
            r * sideBright, g * sideBright, b * sideBright)
        idx += 36

        // BACK face (-Z)
        addFace(vertices, idx, x + 1, y, z, x, y, z, x, y + 1, z, x + 1, y + 1, z,
            r * sideBright, g * sideBright, b * sideBright)
        idx += 36

        // TOP face (+Y)
        addFace(vertices, idx, x, y + 1, z + 1, x + 1, y + 1, z + 1, x + 1, y + 1, z, x, y + 1, z,
            r * topBright, g * topBright, b * topBright)
        idx += 36

        // BOTTOM face (-Y)
        addFace(vertices, idx, x, y, z, x + 1, y, z, x + 1, y, z + 1, x, y, z + 1,
            r * bottomBright, g * bottomBright, b * bottomBright)
        idx += 36

        // LEFT face (-X)
        addFace(vertices, idx, x, y, z, x, y, z + 1, x, y + 1, z + 1, x, y + 1, z,
            r * sideBright, g * sideBright, b * sideBright)
        idx += 36

        // RIGHT face (+X)
        addFace(vertices, idx, x + 1, y, z + 1, x + 1, y, z, x + 1, y + 1, z, x + 1, y + 1, z + 1,
            r * sideBright, g * sideBright, b * sideBright)

        return vertices
    }

    private fun addFace(vertices: FloatArray, startIdx: Int,
                        x0: Float, y0: Float, z0: Float,
                        x1: Float, y1: Float, z1: Float,
                        x2: Float, y2: Float, z2: Float,
                        x3: Float, y3: Float, z3: Float,
                        r: Float, g: Float, b: Float) {
        var idx = startIdx

        // Triangle 1
        vertices[idx++] = x0; vertices[idx++] = y0; vertices[idx++] = z0
        vertices[idx++] = r;  vertices[idx++] = g;  vertices[idx++] = b

        vertices[idx++] = x1; vertices[idx++] = y1; vertices[idx++] = z1
        vertices[idx++] = r;  vertices[idx++] = g;  vertices[idx++] = b

        vertices[idx++] = x2; vertices[idx++] = y2; vertices[idx++] = z2
        vertices[idx++] = r;  vertices[idx++] = g;  vertices[idx++] = b

        // Triangle 2
        vertices[idx++] = x0; vertices[idx++] = y0; vertices[idx++] = z0
        vertices[idx++] = r;  vertices[idx++] = g;  vertices[idx++] = b

        vertices[idx++] = x2; vertices[idx++] = y2; vertices[idx++] = z2
        vertices[idx++] = r;  vertices[idx++] = g;  vertices[idx++] = b

        vertices[idx++] = x3; vertices[idx++] = y3; vertices[idx++] = z3
        vertices[idx++] = r;  vertices[idx++] = g;  vertices[idx++] = b
    }
}
