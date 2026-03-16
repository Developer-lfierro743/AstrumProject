package com.novusforge.astrum.core.ecs.components;

import com.novusforge.astrum.api.ecs.Component;
import org.joml.Vector3f;

/**
 * High-performance Velocity Component.
 * Optimized for cache locality as per "The Formula".
 */
public class VelocityComponent implements Component {
    public final Vector3f velocity = new Vector3f();

    public VelocityComponent(float x, float y, float z) {
        this.velocity.set(x, y, z);
    }
}
