package game.client;

import org.lwjgl.*;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.stb.*;
import org.lwjgl.system.*;
import org.joml.*;
import org.joml.Math;

import java.io.IOException;
import java.io.InputStream;
import java.nio.*;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL45.*;
import static org.lwjgl.stb.STBImage.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

public class tempTriangle {

    // The window handle
    private long window;
    
    // Shader program, vertex data, and texture
    private int shaderProgram;
    private int VAO, VBO, EBO;
    private int texture;
    
    // Matrix uniforms
    private int modelLoc, viewLoc, projectionLoc;
    
    // Matrices
    private Matrix4f model = new Matrix4f();
    private Matrix4f view = new Matrix4f();
    private Matrix4f projection = new Matrix4f();
    private FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(16);
    
    // Camera position and movement
    private Vector3f cameraPos = new Vector3f(0.0f, 3.0f, 0.0f);
    private Vector3f cameraTarget = new Vector3f(0.0f, 0.0f, 0.0f);
    private Vector3f cameraUp = new Vector3f(1.0f, 0.0f, -0.5f);
    private float cameraSpeed = 0.1f;
    
    // Input state
    private boolean[] keys = new boolean[GLFW_KEY_LAST];

    public void run() {
        System.out.println("Hello LWJGL " + Version.getVersion() + "!");

        init();
        loop();

        // Free the window callbacks and destroy the window
        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);

        // Terminate GLFW and free the error callback
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }

    private void init() {
        // Setup an error callback. The default implementation
        // will print the error message in System.err.
        GLFWErrorCallback.createPrint(System.err).set();

        // Initialize GLFW. Most GLFW functions will not work before doing this.
        if ( !glfwInit() )
            throw new IllegalStateException("Unable to initialize GLFW");

        // Configure GLFW
        glfwDefaultWindowHints(); // optional, the current window hints are already the default
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE); // the window will stay hidden after creation
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE); // the window will be resizable
        
        // Request OpenGL 4.5 Core Profile for DSA support
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 5);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);

        // Create the window
        window = glfwCreateWindow(800, 600, "Textured Quad with WASD Camera - OpenGL 4.5 DSA!", NULL, NULL);
        if ( window == NULL )
            throw new RuntimeException("Failed to create the GLFW window");

        // Setup a key callback for input handling
        glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
            if (action == GLFW_PRESS) {
                keys[key] = true;
            } else if (action == GLFW_RELEASE) {
                keys[key] = false;
            }
            
            if ( key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE )
                glfwSetWindowShouldClose(window, true);
        });

        // Get the thread stack and push a new frame
        try ( MemoryStack stack = stackPush() ) {
            IntBuffer pWidth = stack.mallocInt(1); // int*
            IntBuffer pHeight = stack.mallocInt(1); // int*

            // Get the window size passed to glfwCreateWindow
            glfwGetWindowSize(window, pWidth, pHeight);

            // Get the resolution of the primary monitor
            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

            // Center the window
            glfwSetWindowPos(
                window,
                (vidmode.width() - pWidth.get(0)) / 2,
                (vidmode.height() - pHeight.get(0)) / 2
            );
        } // the stack frame is popped automatically

        // Make the OpenGL context current
        glfwMakeContextCurrent(window);

        // Enable v-sync
        glfwSwapInterval(1);

        // Make the window visible
        glfwShowWindow(window);

        // This line is critical for LWJGL's interoperation with GLFW's
        // OpenGL context, or any context that is managed externally.
        // LWJGL detects the context that is current in the current thread,
        // creates the GLCapabilities instance and makes the OpenGL
        // bindings available for use.
        GL.createCapabilities();

        System.out.println("OpenGL Version: " + glGetString(GL_VERSION));
        System.out.println("Graphics Card: " + glGetString(GL_RENDERER));

        setupQuad();
        setupMatrices();
    }

    private void processInput() {
        // WASD camera movement
        if (keys[GLFW_KEY_W]) {
            // Move forward (negative Z in this coordinate system)
            cameraPos.z -= cameraSpeed;
        }
        if (keys[GLFW_KEY_S]) {
            // Move backward (positive Z in this coordinate system)
            cameraPos.z += cameraSpeed;
        }
        if (keys[GLFW_KEY_A]) {
            // Move left (negative X)
            cameraPos.x -= cameraSpeed;
        }
        if (keys[GLFW_KEY_D]) {
            // Move right (positive X)
            cameraPos.x += cameraSpeed;
        }
        
        // Update camera target to maintain relative position
        cameraTarget.set(cameraPos.x, cameraPos.y - 3.0f, cameraPos.z);
    }

    private void setupQuad() {
        // Vertex shader source code - updated with MVP matrices
        String vertexShaderSource = "#version 450 core\n" +
            "layout (location = 0) in vec3 aPos;\n" +
            "layout (location = 1) in vec2 aTexCoord;\n" +
            "\n" +
            "uniform mat4 model;\n" +
            "uniform mat4 view;\n" +
            "uniform mat4 projection;\n" +
            "\n" +
            "out vec2 TexCoord;\n" +
            "\n" +
            "void main()\n" +
            "{\n" +
            "   gl_Position = projection * view * model * vec4(aPos, 1.0);\n" +
            "   TexCoord = aTexCoord;\n" +
            "}\0";

        // Fragment shader source code - updated for texture sampling
        String fragmentShaderSource = "#version 450 core\n" +
            "in vec2 TexCoord;\n" +
            "out vec4 FragColor;\n" +
            "uniform sampler2D ourTexture;\n" +
            "void main()\n" +
            "{\n" +
            "   FragColor = texture(ourTexture, TexCoord);\n" +
            "}\n\0";

        // Compile vertex shader
        int vertexShader = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vertexShader, vertexShaderSource);
        glCompileShader(vertexShader);
        
        // Check for shader compile errors
        int[] success = new int[1];
        glGetShaderiv(vertexShader, GL_COMPILE_STATUS, success);
        if (success[0] == 0) {
            String infoLog = glGetShaderInfoLog(vertexShader);
            System.err.println("ERROR::SHADER::VERTEX::COMPILATION_FAILED\n" + infoLog);
        }

        // Compile fragment shader
        int fragmentShader = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fragmentShader, fragmentShaderSource);
        glCompileShader(fragmentShader);
        
        // Check for shader compile errors
        glGetShaderiv(fragmentShader, GL_COMPILE_STATUS, success);
        if (success[0] == 0) {
            String infoLog = glGetShaderInfoLog(fragmentShader);
            System.err.println("ERROR::SHADER::FRAGMENT::COMPILATION_FAILED\n" + infoLog);
        }

        // Link shaders
        shaderProgram = glCreateProgram();
        glAttachShader(shaderProgram, vertexShader);
        glAttachShader(shaderProgram, fragmentShader);
        glLinkProgram(shaderProgram);
        
        // Check for linking errors
        glGetProgramiv(shaderProgram, GL_LINK_STATUS, success);
        if (success[0] == 0) {
            String infoLog = glGetProgramInfoLog(shaderProgram);
            System.err.println("ERROR::SHADER::PROGRAM::LINKING_FAILED\n" + infoLog);
        }
        
        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);

        // Get uniform locations
        modelLoc = glGetUniformLocation(shaderProgram, "model");
        viewLoc = glGetUniformLocation(shaderProgram, "view");
        projectionLoc = glGetUniformLocation(shaderProgram, "projection");

        // Set up vertex data for a quad (XYZST format: position + texture coordinates)
        float vertices[] = {
            // positions          // texture coords (fixed x-axis)
                    10, 0, 8, 1, 0,  // top right
                    0, 0, 8, 0, 0,   // top left
                    10, 0, 0, 1, 1,  // bottom right
                    0, 0, 0, 0, 1,   // bottom left
        };
        
        // Indices for drawing the quad (two triangles)
        int indices[] = {
            0, 1, 3,   // first triangle
            0, 2, 3    // second triangle
        };

        // DSA Style: Create Vertex Array Object using Direct State Access
        VAO = glCreateVertexArrays();
        
        // DSA Style: Create Buffer Objects using Direct State Access
        VBO = glCreateBuffers();
        EBO = glCreateBuffers();
        
        // DSA Style: Upload vertex data directly without binding
        glNamedBufferData(VBO, vertices, GL_STATIC_DRAW);
        glNamedBufferData(EBO, indices, GL_STATIC_DRAW);
        
        // DSA Style: Bind the buffers to the VAO
        glVertexArrayVertexBuffer(VAO, 0, VBO, 0, 5 * Float.BYTES);
        glVertexArrayElementBuffer(VAO, EBO);
        
        // DSA Style: Configure position attribute (location 0)
        glVertexArrayAttribFormat(VAO, 0, 3, GL_FLOAT, false, 0);
        glVertexArrayAttribBinding(VAO, 0, 0);
        glEnableVertexArrayAttrib(VAO, 0);
        
        // DSA Style: Configure texture coordinate attribute (location 1)
        glVertexArrayAttribFormat(VAO, 1, 2, GL_FLOAT, false, 3 * Float.BYTES);
        glVertexArrayAttribBinding(VAO, 1, 0);
        glEnableVertexArrayAttrib(VAO, 1);

        // Load texture
        loadTexture();

        System.out.println("Quad setup complete using OpenGL 4.5 DSA with MVP matrices!");
    }

    private void setupMatrices() {
        // Initialize matrices
        model.identity();
        
        // Set up projection matrix (orthographic projection)
        projection.ortho(-10.0f, 10.0f, -10.0f, 10.0f, 0.1f, 100.0f);
        
        System.out.println("Matrices initialized!");
    }

    private void updateViewMatrix() {
        // Update view matrix based on current camera position
        view.identity()
            .lookAt(cameraPos.x, cameraPos.y, cameraPos.z,   // camera position
                    cameraTarget.x, cameraTarget.y, cameraTarget.z,   // look at point
                    cameraUp.x, cameraUp.y, cameraUp.z);  // up vector
    }

    private void loadTexture() {
        // DSA Style: Create texture using Direct State Access
        texture = glCreateTextures(GL_TEXTURE_2D);
        
        // Set texture parameters using DSA
        glTextureParameteri(texture, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTextureParameteri(texture, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTextureParameteri(texture, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTextureParameteri(texture, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        try (MemoryStack stack = stackPush()) {
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            // Load texture from resources
            InputStream textureStream = getClass().getResourceAsStream("/textures/textures.png");
            if (textureStream == null) {
                throw new RuntimeException("Could not find texture file: /textures/textures.png");
            }

            byte[] textureData;
            try {
                textureData = textureStream.readAllBytes();
                textureStream.close();
            } catch (IOException e) {
                throw new RuntimeException("Failed to read texture file", e);
            }

            ByteBuffer textureBuffer = memAlloc(textureData.length);
            textureBuffer.put(textureData);
            textureBuffer.flip();

            stbi_set_flip_vertically_on_load(true);
            ByteBuffer image = stbi_load_from_memory(textureBuffer, width, height, channels, 0);
            
            if (image != null) {
                int format = (channels.get(0) == 4) ? GL_RGBA : GL_RGB;
                
                // DSA Style: Upload texture data directly
                glTextureStorage2D(texture, 1, GL_RGBA8, width.get(0), height.get(0));
                glTextureSubImage2D(texture, 0, 0, 0, width.get(0), height.get(0), format, GL_UNSIGNED_BYTE, image);
                
                System.out.println("Texture loaded successfully: " + width.get(0) + "x" + height.get(0) + " channels: " + channels.get(0));
            } else {
                throw new RuntimeException("Failed to load texture");
            }

            stbi_image_free(image);
            memFree(textureBuffer);
        }
    }

    private void loop() {
        // Set the clear color
        glClearColor(0.2f, 0.3f, 0.3f, 1.0f);
        
        // Enable depth testing
        glEnable(GL_DEPTH_TEST);

        // Run the rendering loop until the user has attempted to close
        // the window or has pressed the ESCAPE key.
        while ( !glfwWindowShouldClose(window) ) {
            // Process input
            processInput();
            
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear the framebuffer

            // Update model matrix
            model.identity();

            // Update view matrix based on camera position
            updateViewMatrix();

            // Use shader program
            glUseProgram(shaderProgram);
            
            // Upload matrices to shaders
            matrixBuffer.clear();
            model.get(matrixBuffer);
            glUniformMatrix4fv(modelLoc, false, matrixBuffer);
            
            matrixBuffer.clear();
            view.get(matrixBuffer);
            glUniformMatrix4fv(viewLoc, false, matrixBuffer);
            
            matrixBuffer.clear();
            projection.get(matrixBuffer);
            glUniformMatrix4fv(projectionLoc, false, matrixBuffer);

            // Bind texture
            glBindTextureUnit(0, texture);

            // Draw the quad
            glBindVertexArray(VAO);
            glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0);

            glfwSwapBuffers(window); // swap the color buffers

            // Poll for window events. The key callback above will only be
            // invoked during this call.
            glfwPollEvents();
        }
    }

    public static void main(String[] args) {
        new tempTriangle().run();
    }
}
