package game.client;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL;
import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL20.glDeleteProgram;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.GL_LINK_STATUS;
import static org.lwjgl.opengl.GL20.glGetProgramiv;
import static org.lwjgl.opengl.GL20.glGetProgramInfoLog;
import static org.lwjgl.opengl.GL11.GL_FALSE;
import org.lwjgl.opengl.GL41;
import static org.lwjgl.opengl.GL41.GL_FRAGMENT_SHADER_BIT;
import static org.lwjgl.opengl.GL41.GL_VERTEX_SHADER_BIT;
import static org.lwjgl.opengl.GL41.glBindProgramPipeline;
import static org.lwjgl.opengl.GL41.glDeleteProgramPipelines;
import static org.lwjgl.opengl.GL41.glGenProgramPipelines;
import static org.lwjgl.opengl.GL41.glProgramUniform1i;
import static org.lwjgl.opengl.GL41.glProgramUniformMatrix4fv;
import static org.lwjgl.opengl.GL41.glUseProgramStages;
import static org.lwjgl.opengl.GL45.glBindTextureUnit;
import static org.lwjgl.opengl.GL45.glCreateTextures;
import static org.lwjgl.opengl.GL45.glTextureParameteri;
import static org.lwjgl.opengl.GL45.glTextureStorage2D;
import static org.lwjgl.opengl.GL45.glTextureSubImage2D;
import static org.lwjgl.stb.STBImage.stbi_image_free;
import static org.lwjgl.stb.STBImage.stbi_load_from_memory;
import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_NEAREST;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_REPEAT;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_RGBA8;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;

public class Opengl {
    static int vertex_program;
    static int fragment_program;
    static int pipeline;

    static int modelMatrixLocation;
    static int viewMatrixLocation;      
    static int projectionMatrixLocation;

    static Matrix4f modelMatrix = new Matrix4f();
    static Matrix4f viewMatrix = new Matrix4f();
    static Matrix4f projectionMatrix = new Matrix4f();

    static int textureSamplerUniform;
    static int textureId;
    
    static int atlasWidth;
    static int atlasHeight;
    static ByteBuffer texturePixelData; // CPU copy for pixel reading

    private Opengl(){}//this class will never be instantiated.

    public static void init(){
        GL.createCapabilities();
        // options
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glDisable(GL_CULL_FACE);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        
        // load shaders 
        fragment_program = ShaderFromResource(GL_FRAGMENT_SHADER, "/fragment.glsl");
        vertex_program = ShaderFromResource( GL_VERTEX_SHADER, "/vertex.glsl");
        System.out.println("Checking shader compilation status...");
        int[] status = new int[1];
        glGetProgramiv(vertex_program, GL_LINK_STATUS, status);
        if (status[0] == GL_FALSE) {
            System.err.println("Vertex program failed to compile/link");
            String log = glGetProgramInfoLog(vertex_program);
            System.err.println("Vertex program error log: " + log);
        } else {
            System.out.println("Vertex program compiled successfully");
        }

        glGetProgramiv(fragment_program, GL_LINK_STATUS, status);
        if (status[0] == GL_FALSE) {
            System.err.println("Fragment program failed to compile/link");
            String log = glGetProgramInfoLog(fragment_program);
            System.err.println("Fragment program error log: " + log);
        } else {
            System.out.println("Fragment program compiled successfully");
        }
        // setup pipeline
        pipeline = glGenProgramPipelines();
        glUseProgramStages(pipeline, GL_VERTEX_SHADER_BIT, vertex_program);
        glUseProgramStages(pipeline, GL_FRAGMENT_SHADER_BIT, fragment_program);
        glBindProgramPipeline(pipeline);
        // cache uniform locations
        modelMatrixLocation = glGetUniformLocation(vertex_program, "modelMatrix"); 
        viewMatrixLocation = glGetUniformLocation(vertex_program, "viewMatrix");
        projectionMatrixLocation = glGetUniformLocation(vertex_program, "projectionMatrix");
        textureSamplerUniform = glGetUniformLocation(Opengl.fragment_program, "textureSampler");
    }
    public static int ShaderFromResource(int type, String path) {
        try {
            String code = new String(Main.class.getResourceAsStream(path).readAllBytes());
            return GL41.glCreateShaderProgramv(type, code);
        } catch (IOException e) {
            System.err.println("cannot read shader files: "+e);
            System.exit(1);
            return -1;//unreachable
        }
    }
    public static ByteBuffer ImageBytesFromResource(String path) {
        try {
            byte[] bytes = Opengl.class.getResourceAsStream(path).readAllBytes();
            ByteBuffer buf = BufferUtils.createByteBuffer(bytes.length);
            buf.put(bytes);
            buf.flip();
            return buf;
        } catch (IOException e){
            System.err.println("could not load texure resource!");
            return null;
        }
    }
    public static void updateMatrixUniforms(){
        glProgramUniformMatrix4fv(vertex_program, projectionMatrixLocation, false, projectionMatrix.get(new  float[16]));
        glProgramUniformMatrix4fv(vertex_program, viewMatrixLocation, false, viewMatrix.get(new  float[16]));
        glProgramUniformMatrix4fv(vertex_program, modelMatrixLocation, false, modelMatrix.get(new  float[16]));
    }
    public static void loadTexture(String path) {
        // create
        int id = glCreateTextures(GL_TEXTURE_2D);
        // options
        glTextureParameteri(id, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTextureParameteri(id, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTextureParameteri(id, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTextureParameteri(id, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        // read
        ByteBuffer rawImage = ImageBytesFromResource(path);
        int[] width = new int[1];
        int[] height = new int[1];
        int[] num_channels = new int[1];
        // decode
        ByteBuffer decodedImage = stbi_load_from_memory(rawImage, width, height, num_channels, 4);
        
        // Keep a CPU copy for pixel reading
        texturePixelData = BufferUtils.createByteBuffer(decodedImage.remaining());
        texturePixelData.put(decodedImage);
        texturePixelData.flip();
        decodedImage.rewind(); // Reset position for GPU upload
        
        glTextureStorage2D(id, 1,  GL_RGBA8 , width[0], height[0]); // alocates gpu memory
        glTextureSubImage2D(id, 0, 0, 0, width[0], height[0], 
                            GL_RGBA, GL_UNSIGNED_BYTE, decodedImage); // upload decoded image to gpu
        atlasWidth = width[0];
        atlasHeight = height[0];

        stbi_image_free(decodedImage); // frees from cpu memory
        textureId = id;
        // bind and use
        glBindTextureUnit(0, textureId);
        glProgramUniform1i(fragment_program, textureSamplerUniform, 0);

        // defense
        if (rawImage == null) {
            System.err.println("Failed to load raw image data from: " + path);
            return;
        }
    }
    public static float[] readPixelColor(float s, float t){
        // Check if texture data is available
        if (texturePixelData == null) {
            System.err.println("No texture data available for pixel reading");
            return new float[]{1.0f, 0.0f, 1.0f, 1.0f}; // Return magenta with full alpha
        }
        
        // Clamp s and t to [0, 1] range
        s = Math.max(0.0f, Math.min(1.0f, s));
        t = Math.max(0.0f, Math.min(1.0f, t));
        
        // Convert texture coordinates to pixel coordinates
        int x = (int)(s * (atlasWidth - 1));
        int y = (int)(t * (atlasHeight - 1));
        
        // Calculate byte offset in the buffer (4 bytes per pixel: RGBA)
        int pixelIndex = (y * atlasWidth + x) * 4;
        
        // Extract RGBA values (convert unsigned bytes to floats 0.0-1.0)
        float r = (texturePixelData.get(pixelIndex) & 0xFF) / 255.0f;
        float g = (texturePixelData.get(pixelIndex + 1) & 0xFF) / 255.0f;
        float b = (texturePixelData.get(pixelIndex + 2) & 0xFF) / 255.0f;
        float a = (texturePixelData.get(pixelIndex + 3) & 0xFF) / 255.0f;
        
        return new float[]{r, g, b, a};
    }

    public static void cleanup() {
        glDeleteProgram(vertex_program);
        glDeleteProgram(fragment_program);
        glDeleteProgramPipelines(pipeline);
    }
}
