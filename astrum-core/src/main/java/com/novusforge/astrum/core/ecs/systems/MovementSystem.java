package com.novusforge.astrum.core.ecs.systems;

import com.novusforge.astrum.core.ecs.ECSSystem;
import com.novusforge.astrum.core.ecs.ECSSystemManager;
import com.novusforge.astrum.core.ecs.components.PositionComponent;
import com.novusforge.astrum.core.ecs.components.VelocityComponent;

import java.util.List;

/**
 * Movement System - processes velocity and position components.
 * Applies velocity to position each frame.
 */
public class MovementSystem implements ECSSystem {
    
    private static final float DEFAULT_GRAVITY = -9.81f;
    private static final float TERMINAL_VELOCITY = -78.4f;
    private float gravity = DEFAULT_GRAVITY;
    private boolean gravityEnabled = true;
    
    public MovementSystem() {}
    
    public MovementSystem(float customGravity) {
        this.gravity = customGravity;
    }
    
    @Override
    public void update(ECSSystemManager manager, float deltaTime) {
        List<Integer> entities = manager.getEntitiesWith(PositionComponent.class);
        
        for (int entityId : entities) {
            PositionComponent position = manager.getComponent(entityId, PositionComponent.class);
            VelocityComponent velocity = manager.getComponent(entityId, VelocityComponent.class);
            
            if (velocity != null) {
                applyVelocity(position, velocity, deltaTime);
            }
        }
    }
    
    private void applyVelocity(PositionComponent position, VelocityComponent velocity, float deltaTime) {
        position.position.x += velocity.velocity.x * deltaTime;
        position.position.y += velocity.velocity.y * deltaTime;
        position.position.z += velocity.velocity.z * deltaTime;
        
        if (gravityEnabled) {
            velocity.velocity.y += gravity * deltaTime;
            if (velocity.velocity.y < TERMINAL_VELOCITY) {
                velocity.velocity.y = TERMINAL_VELOCITY;
            }
        }
    }
    
    public void setGravity(float gravity) {
        this.gravity = gravity;
    }
    
    public void setGravityEnabled(boolean enabled) {
        this.gravityEnabled = enabled;
    }
    
    public float getGravity() {
        return gravity;
    }
    
    public boolean isGravityEnabled() {
        return gravityEnabled;
    }
}
