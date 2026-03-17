package com.novusforge.astrum.engine.render;

import org.joml.Vector3f;

/**
 * First Cube renderer - The "Hello World" of 3D!
 * Renders a simple colored cube to verify the rendering pipeline.
 */
public class FirstCube {
    
    public static class CubeMesh {
        public float[] positions;
        public float[] colors;
        public int vertexCount;
        
        public CubeMesh(float[] positions, float[] colors, int vertexCount) {
            this.positions = positions;
            this.colors = colors;
            this.vertexCount = vertexCount;
        }
    }
    
    public static CubeMesh generateCube() {
        float x = 0, y = 0, z = 0;
        float size = 0.5f;
        
        float x0 = x - size, x1 = x + size;
        float y0 = y - size, y1 = y + size;
        float z0 = z - size, z1 = z + size;
        
        float[] positions = {
            // Front face (z+)
            x1, y1, z1,  x0, y1, z1,  x0, y0, z1,  x1, y0, z1,
            x1, y1, z1,  x0, y0, z1,  x1, y0, z1,  x0, y1, z1,
            
            // Back face (z-)
            x0, y1, z0,  x1, y1, z0,  x1, y0, z0,  x0, y0, z0,
            x0, y1, z0,  x1, y0, z0,  x0, y0, z0,  x1, y1, z0,
            
            // Top face (y+)
            x0, y1, z1,  x1, y1, z0,  x1, y1, z1,  x0, y1, z0,
            
            // Bottom face (y-)
            x0, y0, z0,  x1, y0, z1,  x1, y0, z0,  x0, y0, z1,
            
            // Right face (x+)
            x1, y1, z0,  x1, y1, z1,  x1, y0, z1,  x1, y0, z0,
            x1, y1, z0,  x1, y0, z1,  x1, y0, z0,  x1, y1, z1,
            
            // Left face (x-)
            x0, y1, z1,  x0, y1, z0,  x0, y0, z0,  x0, y0, z1,
            x0, y1, z1,  x0, y0, z0,  x0, y0, z1,  x0, y1, z0,
        };
        
        float[] colors = {
            // Front - Vibrant Green
            0.0f, 1.0f, 0.0f,  0.0f, 1.0f, 0.0f,  0.0f, 1.0f, 0.0f,  0.0f, 1.0f, 0.0f,
            0.0f, 1.0f, 0.0f,  0.0f, 1.0f, 0.0f,  0.0f, 1.0f, 0.0f,  0.0f, 1.0f, 0.0f,
            
            // Back - Vibrant Blue
            0.0f, 0.0f, 1.0f,  0.0f, 0.0f, 1.0f,  0.0f, 0.0f, 1.0f,  0.0f, 0.0f, 1.0f,
            0.0f, 0.0f, 1.0f,  0.0f, 0.0f, 1.0f,  0.0f, 0.0f, 1.0f,  0.0f, 0.0f, 1.0f,
            
            // Top - Vibrant Yellow
            1.0f, 1.0f, 0.0f,  1.0f, 1.0f, 0.0f,  1.0f, 1.0f, 0.0f,  1.0f, 1.0f, 0.0f,
            
            // Bottom - Vibrant Brown
            0.6f, 0.4f, 0.2f,  0.6f, 0.4f, 0.2f,  0.6f, 0.4f, 0.2f,  0.6f, 0.4f, 0.2f,
            
            // Right - Vibrant Red
            1.0f, 0.0f, 0.0f,  1.0f, 0.0f, 0.0f,  1.0f, 0.0f, 0.0f,  1.0f, 0.0f, 0.0f,
            1.0f, 0.0f, 0.0f,  1.0f, 0.0f, 0.0f,  1.0f, 0.0f, 0.0f,  1.0f, 0.0f, 0.0f,
            
            // Left - Vibrant Magenta
            1.0f, 0.0f, 1.0f,  1.0f, 0.0f, 1.0f,  1.0f, 0.0f, 1.0f,  1.0f, 0.0f, 1.0f,
            1.0f, 0.0f, 1.0f,  1.0f, 0.0f, 1.0f,  1.0f, 0.0f, 1.0f,  1.0f, 0.0f, 1.0f,
        };
        
        return new CubeMesh(positions, colors, positions.length / 3);
    }
    
    public static CubeMesh generateColoredCube() {
        float x = 0, y = 0, z = 0;
        float size = 0.5f;
        
        float x0 = x - size, x1 = x + size;
        float y0 = y - size, y1 = y + size;
        float z0 = z - size, z1 = z + size;
        
        float[] positions = {
            // Front face (z+) - GREEN
            x1, y1, z1,  x0, y1, z1,  x0, y0, z1,  x1, y0, z1,
            x1, y1, z1,  x0, y0, z1,  x1, y0, z1,  x0, y1, z1,
            
            // Back face (z-) - BLUE  
            x0, y1, z0,  x1, y1, z0,  x1, y0, z0,  x0, y0, z0,
            x0, y1, z0,  x1, y0, z0,  x0, y0, z0,  x1, y1, z0,
            
            // Top face (y+) - WHITE
            x0, y1, z1,  x1, y1, z0,  x1, y1, z1,  x0, y1, z0,
            
            // Bottom face (y-) - GREY
            x0, y0, z0,  x1, y0, z1,  x1, y0, z0,  x0, y0, z1,
            
            // Right face (x+) - RED
            x1, y1, z0,  x1, y1, z1,  x1, y0, z1,  x1, y0, z0,
            x1, y1, z0,  x1, y0, z1,  x1, y0, z0,  x1, y1, z1,
            
            // Left face (x-) - CYAN
            x0, y1, z1,  x0, y1, z0,  x0, y0, z0,  x0, y0, z1,
            x0, y1, z1,  x0, y0, z0,  x0, y0, z1,  x0, y1, z0,
        };
        
        float[] colors = {
            // Front - Green
            0.2f, 0.9f, 0.2f,  0.2f, 0.9f, 0.2f,  0.2f, 0.9f, 0.2f,  0.2f, 0.9f, 0.2f,
            0.2f, 0.9f, 0.2f,  0.2f, 0.9f, 0.2f,  0.2f, 0.9f, 0.2f,  0.2f, 0.9f, 0.2f,
            
            // Back - Blue
            0.2f, 0.2f, 0.9f,  0.2f, 0.2f, 0.9f,  0.2f, 0.2f, 0.9f,  0.2f, 0.2f, 0.9f,
            0.2f, 0.2f, 0.9f,  0.2f, 0.2f, 0.9f,  0.2f, 0.2f, 0.9f,  0.2f, 0.2f, 0.9f,
            
            // Top - White
            0.9f, 0.9f, 0.9f,  0.9f, 0.9f, 0.9f,  0.9f, 0.9f, 0.9f,  0.9f, 0.9f, 0.9f,
            
            // Bottom - Grey
            0.4f, 0.4f, 0.4f,  0.4f, 0.4f, 0.4f,  0.4f, 0.4f, 0.4f,  0.4f, 0.4f, 0.4f,
            
            // Right - Red
            0.9f, 0.2f, 0.2f,  0.9f, 0.2f, 0.2f,  0.9f, 0.2f, 0.2f,  0.9f, 0.2f, 0.2f,
            0.9f, 0.2f, 0.2f,  0.9f, 0.2f, 0.2f,  0.9f, 0.2f, 0.2f,  0.9f, 0.2f, 0.2f,
            
            // Left - Cyan
            0.2f, 0.9f, 0.9f,  0.2f, 0.9f, 0.9f,  0.2f, 0.9f, 0.9f,  0.2f, 0.9f, 0.9f,
            0.2f, 0.9f, 0.9f,  0.2f, 0.9f, 0.9f,  0.2f, 0.9f, 0.9f,  0.2f, 0.9f, 0.9f,
        };
        
        return new CubeMesh(positions, colors, positions.length / 3);
    }
    
    public static void main(String[] args) {
        System.out.println("=== Project Astrum: First Cube ===");
        
        CubeMesh cube = generateColoredCube();
        
        System.out.println("Generated cube with " + cube.vertexCount + " vertices");
        System.out.println("Positions array length: " + cube.positions.length);
        System.out.println("Colors array length: " + cube.colors.length);
        
        System.out.println("\nFirst vertex: (" + cube.positions[0] + ", " + cube.positions[1] + ", " + cube.positions[2] + ")");
        System.out.println("First color: (" + cube.colors[0] + ", " + cube.colors[1] + ", " + cube.colors[2] + ")");
        
        System.out.println("\n=== First Cube Ready for GPU! ===");
    }
}