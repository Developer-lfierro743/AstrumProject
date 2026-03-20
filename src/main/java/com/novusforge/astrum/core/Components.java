package com.novusforge.astrum.core;

import org.joml.Vector3f;

public class Components {
    public static record Position(Vector3f value) {}
    public static record Velocity(Vector3f value) {}
    public static record Rotation(Vector3f value) {}
    public static record PlayerTag() {}
}
