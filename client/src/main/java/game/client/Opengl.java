package game.client;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL;
import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL20.glDeleteProgram;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glGetUniformfv;
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
import static org.lwjgl.stb.STBImage.stbi_failure_reason;
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
import static org.lwjgl.opengl.GL11.glDepthMask;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;

public class Opengl {
    static int vertex_program;
    static int fragment_program;
    static int pipeline;

    static int modelMatrix;     
    static int viewMatrix;      
    static int projectionMatrix;

    static int textureSamplerUniform;
    static int textureId;
    
    static int atlasWidth;
    static int atlasHeight;

    @SuppressWarnings("unused")
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
        modelMatrix = glGetUniformLocation(vertex_program, "modelMatrix"); 
        viewMatrix = glGetUniformLocation(vertex_program, "viewMatrix");
        projectionMatrix = glGetUniformLocation(vertex_program, "projectionMatrix");
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
    public static void setModelMatrix(Matrix4f matrix) {
        glProgramUniformMatrix4fv(
            vertex_program, 
            modelMatrix,
            false, // transpose
            matrix.get(new  float[16])
        );
    }
    public static Matrix4f getModelMatrix() {
        FloatBuffer buffer = BufferUtils.createFloatBuffer(16);
        glGetUniformfv(
            vertex_program,
            modelMatrix,
            buffer 
        );
        return new Matrix4f(buffer);
    }
    public static void setViewMatrix(Matrix4f matrix) {
        glProgramUniformMatrix4fv(
            vertex_program, 
            viewMatrix,
            false, // transpose
            matrix.get(new  float[16])
        );
    }
    public static Matrix4f getViewMatrix() {
        FloatBuffer buffer = BufferUtils.createFloatBuffer(16);
        glGetUniformfv(
            vertex_program,
            viewMatrix,
            buffer 
        );
        return new Matrix4f(buffer);
    }
    public static void setProjectionMatrix(Matrix4f matrix) {
        glProgramUniformMatrix4fv(
            vertex_program, 
            projectionMatrix,
            false, // transpose
            matrix.get(new  float[16])
        );    
    }
    public static Matrix4f getProjectioMatrix() {
        FloatBuffer buffer = BufferUtils.createFloatBuffer(16);
        glGetUniformfv(
            vertex_program,
            projectionMatrix,
            buffer 
        );
        return new Matrix4f(buffer);
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
        if (decodedImage == null) {
            System.err.println("STB failed to decode image: " + stbi_failure_reason());
            return;
        }
    }
    public static Vector3f getScreenSpace(Vector3f position) {
        Vector3f screenSpace = new Vector3f();
        new Vector4f(position, 1.0f)
            .mul(Opengl.getModelMatrix())
            .mul(Opengl.getViewMatrix())
            .mul(Opengl.getProjectioMatrix())
            .xyz(screenSpace);
        return screenSpace;
    }

    public static void cleanup() {
        glDeleteProgram(vertex_program);
        glDeleteProgram(fragment_program);
        glDeleteProgramPipelines(pipeline);
    }
}
