package com.novusforge.astrum.engine;

import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.util.Map;

import com.novusforge.astrum.world.ChunkMesh;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33C.*;
import static org.lwjgl.system.MemoryUtil.*;

/**
 * OpenGL 3.3 Renderer - Simple, working graphics backend
 * 
 * Much simpler than Vulkan!
 * - No command buffers
 * - No swapchain management  
 * - No semaphores/fences
 * - Just draw and go!
 */
public class OpenGLRenderer implements IRenderer {
    
    private long window;
    private int vao;
    private int vbo;
    private int ebo;
    private int shaderProgram;
    
    // Uniform locations
    private int projMatrixLoc;
    private int viewMatrixLoc;
    private int modelMatrixLoc;
    
    // Test cube
    private int testCubeVAO;
    private int testCubeVBO;
    private boolean renderTestCube = false;
    
    private float aspectRatio = 16f / 9f;
    private int width = 1280;
    private int height = 720;

    @Override
    public boolean init() {
        // Configure GLFW for OpenGL 3.3 Core Profile
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL_TRUE);
        
        // Create window
        window = glfwCreateWindow(width, height, "Astrum - OpenGL", 0, 0);
        if (window == 0) {
            throw new RuntimeException("Failed to create OpenGL window");
        }
        
        // Make context current
        glfwMakeContextCurrent(window);
        glfwSwapInterval(1); // Enable vsync
        
        // Initialize OpenGL
        org.lwjgl.opengl.GL.createCapabilities();
        
        System.out.println("[OpenGL] Version: " + glGetString(GL_VERSION));
        System.out.println("[OpenGL] Renderer: " + glGetString(GL_RENDERER));
        System.out.println("[OpenGL] GLSL: " + glGetString(GL_SHADING_LANGUAGE_VERSION));
        
        // Enable depth testing
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LESS);
        
        // Enable face culling
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        glFrontFace(GL_CCW);
        
        // Set clear color (sky blue)
        glClearColor(0.4f, 0.6f, 1.0f, 1.0f);
        
        // Create shaders
        createShaders();
        
        // Create test cube
        createTestCube();
        
        System.out.println("[OpenGL] Initialized successfully!");
        return true;
    }

    private void createShaders() {
        // Vertex shader
        String vertexSource = "#version 330 core\n" +
            "layout (location = 0) in vec3 position;\n" +
            "layout (location = 1) in vec3 color;\n" +
            "uniform mat4 projection;\n" +
            "uniform mat4 view;\n" +
            "uniform mat4 model;\n" +
            "out vec3 fragColor;\n" +
            "void main() {\n" +
            "    gl_Position = projection * view * model * vec4(position, 1.0);\n" +
            "    fragColor = color;\n" +
            "}";
        
        // Fragment shader
        String fragmentSource = "#version 330 core\n" +
            "in vec3 fragColor;\n" +
            "out vec4 outColor;\n" +
            "void main() {\n" +
            "    outColor = vec4(fragColor, 1.0);\n" +
            "}";
        
        // Compile shaders
        int vertexShader = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vertexShader, vertexSource);
        glCompileShader(vertexShader);
        
        if (glGetShaderi(vertexShader, GL_COMPILE_STATUS) == GL_FALSE) {
            throw new RuntimeException("Vertex shader failed: " + glGetShaderInfoLog(vertexShader));
        }
        
        int fragmentShader = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fragmentShader, fragmentSource);
        glCompileShader(fragmentShader);
        
        if (glGetShaderi(fragmentShader, GL_COMPILE_STATUS) == GL_FALSE) {
            throw new RuntimeException("Fragment shader failed: " + glGetShaderInfoLog(fragmentShader));
        }
        
        // Link program
        shaderProgram = glCreateProgram();
        glAttachShader(shaderProgram, vertexShader);
        glAttachShader(shaderProgram, fragmentShader);
        glLinkProgram(shaderProgram);
        
        if (glGetProgrami(shaderProgram, GL_LINK_STATUS) == GL_FALSE) {
            throw new RuntimeException("Shader program failed: " + glGetProgramInfoLog(shaderProgram));
        }
        
        // Cleanup shaders
        glDetachShader(shaderProgram, vertexShader);
        glDetachShader(shaderProgram, fragmentShader);
        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);
        
        // Get uniform locations
        glUseProgram(shaderProgram);
        projMatrixLoc = glGetUniformLocation(shaderProgram, "projection");
        viewMatrixLoc = glGetUniformLocation(shaderProgram, "view");
        modelMatrixLoc = glGetUniformLocation(shaderProgram, "model");
        
        System.out.println("[OpenGL] Shader program created");
    }

    private void createTestCube() {
        // Generate cube vertices
        float[] vertices = CubeMesh.generateTexturedCube(0.5f, 0.5f, 0.5f, 0.8f, 0.4f, 0.2f);
        
        // Create VAO
        int[] vaos = new int[1];
        glGenVertexArrays(vaos);
        testCubeVAO = vaos[0];
        glBindVertexArray(testCubeVAO);
        
        // Create VBO
        int[] vbos = new int[1];
        glGenBuffers(vbos);
        testCubeVBO = vbos[0];
        glBindBuffer(GL_ARRAY_BUFFER, testCubeVBO);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);
        
        // Position attribute (location = 0)
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 6 * 4, 0);
        glEnableVertexAttribArray(0);
        
        // Color attribute (location = 1)
        glVertexAttribPointer(1, 3, GL_FLOAT, false, 6 * 4, 3 * 4);
        glEnableVertexAttribArray(1);
        
        glBindVertexArray(0);
        
        System.out.println("[OpenGL] Test cube created: " + (vertices.length / 6) + " vertices");
    }

    @Override
    public void render(Matrix4f view, Matrix4f projection, Map<Long, ChunkMesh> meshes) {
        // Clear screen
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        
        // Use shader program
        glUseProgram(shaderProgram);
        
        // Set matrices
        glUniformMatrix4fv(projMatrixLoc, false, projection.get(new float[16]));
        glUniformMatrix4fv(viewMatrixLoc, false, view.get(new float[16]));
        glUniformMatrix4fv(modelMatrixLoc, false, new Matrix4f().identity().get(new float[16]));
        
        // Render test cube
        if (renderTestCube && testCubeVAO != 0) {
            glBindVertexArray(testCubeVAO);
            glDrawArrays(GL_TRIANGLES, 0, 36);
            glBindVertexArray(0);
        }
        
        // Render chunk meshes (stub for now)
        // TODO: Implement chunk rendering
        
        // Swap buffers
        glfwSwapBuffers(window);
        
        // Poll events
        glfwPollEvents();
    }

    @Override
    public boolean windowShouldClose() {
        return glfwWindowShouldClose(window);
    }

    @Override
    public long getWindow() {
        return window;
    }

    @Override
    public float getAspectRatio() {
        return aspectRatio;
    }

    @Override
    public void setRenderTestCube(boolean render) {
        this.renderTestCube = render;
    }

    @Override
    public void cleanup() {
        if (window != 0) {
            glDeleteVertexArrays(testCubeVAO);
            glDeleteBuffers(testCubeVBO);
            glDeleteProgram(shaderProgram);
            glfwDestroyWindow(window);
        }
    }

    @Override
    public void deleteBuffer(long bufferId, long memoryId) {
        if (bufferId != 0) {
            int[] buffers = new int[] { (int) bufferId };
            glDeleteBuffers(buffers);
        }
    }

    @Override
    public String getRendererName() {
        return "OpenGL 3.3";
    }

    @Override
    public int getFPS() {
        return 60;
    }

    @Override
    public int getLoadedChunkCount() {
        return 0;
    }
}
