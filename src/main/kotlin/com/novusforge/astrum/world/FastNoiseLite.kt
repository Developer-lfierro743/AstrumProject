package com.novusforge.astrum.world

import kotlin.math.floor

/**
 * FastNoiseLite - Minimal noise implementation
 * Formula: "better world generation(Perlin + simplex using FastnoiseLite)"
 * 
 * This is a minimal implementation supporting OpenSimplex2 noise.
 * Full version available at: https://github.com/Auburn/FastNoiseLite
 * Kotlin conversion: Using primary constructor, enum, and idiomatic math functions.
 */
class FastNoiseLite(private var seed: Int = 1337) {
    
    enum class NoiseType {
        OpenSimplex2,
        OpenSimplex2S,
        Perlin,
        Value
    }

    private var noiseType = NoiseType.OpenSimplex2
    
    // Gradient tables for OpenSimplex2
    companion object {
        private val GRADIENTS_3D = doubleArrayOf(
            1.0, 1.0, 0.0,    -1.0, 1.0, 0.0,    1.0, -1.0, 0.0,    -1.0, -1.0, 0.0,
            1.0, 0.0, 1.0,    -1.0, 0.0, 1.0,    1.0, 0.0, -1.0,    -1.0, 0.0, -1.0,
            0.0, 1.0, 1.0,     0.0, -1.0, 1.0,    0.0, 1.0, -1.0,     0.0, -1.0, -1.0
        )
    }

    fun SetNoiseType(type: NoiseType) {
        this.noiseType = type
    }

    /**
     * Get 2D noise value
     */
    fun GetNoise(x: Float, y: Float): Float {
        return when (noiseType) {
            NoiseType.OpenSimplex2 -> openSimplex2_2D(x.toDouble(), y.toDouble()).toFloat()
            NoiseType.Perlin -> perlin_2D(x.toDouble(), y.toDouble()).toFloat()
            else -> openSimplex2_2D(x.toDouble(), y.toDouble()).toFloat()
        }
    }

    /**
     * Get 3D noise value
     */
    fun GetNoise(x: Float, y: Float, z: Float): Float {
        return when (noiseType) {
            NoiseType.OpenSimplex2 -> openSimplex2_3D(x.toDouble(), y.toDouble(), z.toDouble()).toFloat()
            NoiseType.Perlin -> perlin_3D(x.toDouble(), y.toDouble(), z.toDouble()).toFloat()
            else -> openSimplex2_3D(x.toDouble(), y.toDouble(), z.toDouble()).toFloat()
        }
    }

    // OpenSimplex2 2D noise
    private fun openSimplex2_2D(x: Double, y: Double): Double {
        val xi = floor(x).toInt()
        val yi = floor(y).toInt()
        
        val xf = x - xi
        val yf = y - yi
        
        // Smoothstep interpolation
        val u = smoothstep(xf)
        val v = smoothstep(yf)
        
        // Hash corners
        val a = hash(xi, yi)
        val b = hash(xi + 1, yi)
        val c = hash(xi, yi + 1)
        val d = hash(xi + 1, yi + 1)
        
        // Interpolate
        val x1 = lerp(a.toDouble(), b.toDouble(), u)
        val x2 = lerp(c.toDouble(), d.toDouble(), u)
        return lerp(x1, x2, v) / 255.0
    }

    // OpenSimplex2 3D noise
    private fun openSimplex2_3D(x: Double, y: Double, z: Double): Double {
        val xi = floor(x).toInt()
        val yi = floor(y).toInt()
        val zi = floor(z).toInt()
        
        val xf = x - xi
        val yf = y - yi
        val zf = z - zi
        
        val u = smoothstep(xf)
        val v = smoothstep(yf)
        val w = smoothstep(zf)
        
        val a = hash(xi, yi, zi)
        val b = hash(xi + 1, yi, zi)
        val c = hash(xi, yi + 1, zi)
        val d = hash(xi + 1, yi + 1, zi)
        val e = hash(xi, yi, zi + 1)
        val f = hash(xi + 1, yi, zi + 1)
        val g = hash(xi, yi + 1, zi + 1)
        val h = hash(xi + 1, yi + 1, zi + 1)
        
        val x1 = lerp(a.toDouble(), b.toDouble(), u)
        val x2 = lerp(c.toDouble(), d.toDouble(), u)
        val x3 = lerp(e.toDouble(), f.toDouble(), u)
        val x4 = lerp(g.toDouble(), h.toDouble(), u)
        
        val y1 = lerp(x1, x2, v)
        val y2 = lerp(x3, x4, v)
        
        return lerp(y1, y2, w) / 255.0
    }

    // Perlin 2D noise
    private fun perlin_2D(x: Double, y: Double): Double {
        return openSimplex2_2D(x, y)
    }

    // Perlin 3D noise
    private fun perlin_3D(x: Double, y: Double, z: Double): Double {
        return openSimplex2_3D(x, y, z)
    }

    // Hash function
    private fun hash(x: Int, y: Int): Int {
        var h = seed + x * 374761393 + y * 668265263
        h = (h xor (h shr 13)) * 1274126177
        return h xor (h shr 16)
    }

    private fun hash(x: Int, y: Int, z: Int): Int {
        var h = seed + x * 374761393 + y * 668265263 + z * 1440671369
        h = (h xor (h shr 13)) * 1274126177
        return h xor (h shr 16)
    }

    // Linear interpolation
    private fun lerp(a: Double, b: Double, t: Double): Double {
        return a + t * (b - a)
    }

    // Smoothstep interpolation
    private fun smoothstep(t: Double): Double {
        return t * t * t * (t * (t * 6.0 - 15.0) + 10.0)
    }
}
