package com.novusforge.astrum.core.ecs.components;

import com.novusforge.astrum.api.ecs.Component;
import org.joml.Vector3f;

/**
 * High-performance Position Component.
 * Optimized for cache locality as per "The Formula".
 */
public class PositionComponent implements Component {
    public final Vector3f position = new Vector3f();

    public PositionComponent(float x, float y, float z) {
        this.position.set(x, y, z);
    }
}
