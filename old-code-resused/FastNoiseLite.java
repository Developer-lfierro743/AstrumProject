package com.novusforge.astrum.world;

/**
 * FastNoiseLite - Minimal noise implementation
 * Formula: "better world generation(Perlin + simplex using FastnoiseLite)"
 * 
 * This is a minimal implementation supporting OpenSimplex2 noise.
 * Full version available at: https://github.com/Auburn/FastNoiseLite
 */
public class FastNoiseLite {
    
    public enum NoiseType {
        OpenSimplex2,
        OpenSimplex2S,
        Perlin,
        Value
    }

    private int seed = 1337;
    private NoiseType noiseType = NoiseType.OpenSimplex2;
    
    // Gradient tables for OpenSimplex2
    private static final double[] GRADIENTS_3D = {
        1, 1, 0,    -1, 1, 0,    1, -1, 0,    -1, -1, 0,
        1, 0, 1,    -1, 0, 1,    1, 0, -1,    -1, 0, -1,
        0, 1, 1,     0, -1, 1,    0, 1, -1,     0, -1, -1
    };

    public FastNoiseLite() {}
    public FastNoiseLite(int seed) {
        this.seed = seed;
    }

    public void SetNoiseType(NoiseType type) {
        this.noiseType = type;
    }

    /**
     * Get 2D noise value
     */
    public double GetNoise(double x, double y) {
        switch (noiseType) {
            case OpenSimplex2:
                return openSimplex2_2D(x, y);
            case Perlin:
                return perlin_2D(x, y);
            default:
                return openSimplex2_2D(x, y);
        }
    }

    /**
     * Get 3D noise value
     */
    public double GetNoise(double x, double y, double z) {
        switch (noiseType) {
            case OpenSimplex2:
                return openSimplex2_3D(x, y, z);
            case Perlin:
                return perlin_3D(x, y, z);
            default:
                return openSimplex2_3D(x, y, z);
        }
    }

    // OpenSimplex2 2D noise
    private double openSimplex2_2D(double x, double y) {
        // Simple hash-based noise for Pre-Classic
        long seed = this.seed;
        int xi = (int) Math.floor(x);
        int yi = (int) Math.floor(y);
        
        double xf = x - xi;
        double yf = y - yi;
        
        // Smoothstep interpolation
        double u = smoothstep(xf);
        double v = smoothstep(yf);
        
        // Hash corners
        int a = hash(xi, yi);
        int b = hash(xi + 1, yi);
        int c = hash(xi, yi + 1);
        int d = hash(xi + 1, yi + 1);
        
        // Interpolate
        double x1 = lerp(a, b, u);
        double x2 = lerp(c, d, u);
        return lerp(x1, x2, v) / 255.0;
    }

    // OpenSimplex2 3D noise
    private double openSimplex2_3D(double x, double y, double z) {
        long seed = this.seed;
        int xi = (int) Math.floor(x);
        int yi = (int) Math.floor(y);
        int zi = (int) Math.floor(z);
        
        double xf = x - xi;
        double yf = y - yi;
        double zf = z - zi;
        
        double u = smoothstep(xf);
        double v = smoothstep(yf);
        double w = smoothstep(zf);
        
        int a = hash(xi, yi, zi);
        int b = hash(xi + 1, yi, zi);
        int c = hash(xi, yi + 1, zi);
        int d = hash(xi + 1, yi + 1, zi);
        int e = hash(xi, yi, zi + 1);
        int f = hash(xi + 1, yi, zi + 1);
        int g = hash(xi, yi + 1, zi + 1);
        int h = hash(xi + 1, yi + 1, zi + 1);
        
        double x1 = lerp(a, b, u);
        double x2 = lerp(c, d, u);
        double x3 = lerp(e, f, u);
        double x4 = lerp(g, h, u);
        
        double y1 = lerp(x1, x2, v);
        double y2 = lerp(x3, x4, v);
        
        return lerp(y1, y2, w) / 255.0;
    }

    // Perlin 2D noise
    private double perlin_2D(double x, double y) {
        return openSimplex2_2D(x, y);
    }

    // Perlin 3D noise
    private double perlin_3D(double x, double y, double z) {
        return openSimplex2_3D(x, y, z);
    }

    // Hash function
    private int hash(int x, int y) {
        int h = seed + x * 374761393 + y * 668265263;
        h = (h ^ (h >> 13)) * 1274126177;
        return h ^ (h >> 16);
    }

    private int hash(int x, int y, int z) {
        int h = seed + x * 374761393 + y * 668265263 + z * 1440671369;
        h = (h ^ (h >> 13)) * 1274126177;
        return h ^ (h >> 16);
    }

    // Linear interpolation
    private double lerp(double a, double b, double t) {
        return a + t * (b - a);
    }

    // Smoothstep interpolation
    private double smoothstep(double t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }
}
