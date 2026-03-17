package com.novusforge.astrum.api.entity;

/**
 * Base interface for all entities in the modding API.
 */
public interface IEntity {
    
    /**
     * Get unique entity ID
     */
    int getId();
    
    /**
     * Get entity type string
     */
    String getType();
    
    /**
     * Get X position
     */
    float getX();
    
    /**
     * Get Y position
     */
    float getY();
    
    /**
     * Get Z position
     */
    float getZ();
    
    /**
     * Set position
     */
    void setPosition(float x, float y, float z);
    
    /**
     * Get rotation Y (yaw)
     */
    float getRotationY();
    
    /**
     * Get rotation X (pitch)
     */
    float getRotationX();
    
    /**
     * Set rotation
     */
    void setRotation(float yaw, float pitch);
    
    /**
     * Get velocity X
     */
    float getVelocityX();
    
    /**
     * Get velocity Y
     */
    float getVelocityY();
    
    /**
     * Get velocity Z
     */
    float getVelocityZ();
    
    /**
     * Set velocity
     */
    void setVelocity(float x, float y, float z);
    
    /**
     * Check if entity is on ground
     */
    boolean isOnGround();
    
    /**
     * Check if entity is alive
     */
    boolean isAlive();
    
    /**
     * Get entity world
     */
    String getWorldName();
    
    /**
     * Remove entity from world
     */
    void remove();
    
    /**
     * Get custom NBT data
     */
    Object getNBT();
    
    /**
     * Set custom NBT data
     */
    void setNBT(Object nbt);
}
