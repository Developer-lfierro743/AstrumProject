package com.novusforge.astrum.engine;

public interface GPUResourceManager {
    void deleteBuffer(long bufferId);
    void deleteImage(long imageId);
}
