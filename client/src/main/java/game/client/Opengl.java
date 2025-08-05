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
import static org.lwjgl.opengl.GL11.GL_NEAREST;
import static org.lwjgl.opengl.GL11.GL_REPEAT;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_RGBA8;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL20.glUniform1i;

public class Opengl {
    static int vertex_program;
    static int fragment_program;
    static int pipeline;

    static int modelMatrix;     
    static int viewMatrix;      
    static int projectionMatrix;

    static int textureSamplerUniform;
    static int textureId;

    @SuppressWarnings("unused")
    private Opengl(){}//this class will never be instantiated.

    public static void init(){
        GL.createCapabilities();
        // load shaders 
        fragment_program = ShaderFromResource(GL_FRAGMENT_SHADER, "/fragment.glsl");
        vertex_program = ShaderFromResource( GL_VERTEX_SHADER, "/vertex.glsl");
        // set up pipeline
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
        
        int id = glCreateTextures(GL_TEXTURE_2D);

        glTextureParameteri(id, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTextureParameteri(id, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTextureParameteri(id, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTextureParameteri(id, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        ByteBuffer rawImage = ImageBytesFromResource(path);
        if (rawImage == null) {
            System.err.println("Failed to load raw image data from: " + path);
            return;
        }
        System.out.println("raw image data:\n" + rawImage);

        int[] width = new int[1];
        int[] height = new int[1];
        int[] num_channels = new int[1];

        ByteBuffer decodedImage = stbi_load_from_memory(rawImage, width, height, num_channels, 4);
        if (decodedImage == null) {
            System.err.println("STB failed to decode image: " + stbi_failure_reason());
            return;
        }
        
        System.out.println("decoded image data: \n"+ decodedImage + "\n" + width[0] + " " + height[0] + " " + num_channels[0]); 
        glTextureStorage2D(id, 1,  GL_RGBA8 , width[0], height[0]); // alocates gpu memory
        glTextureSubImage2D(id, 0, 0, 0, width[0], height[0], 
                            GL_RGBA, GL_UNSIGNED_BYTE, decodedImage); // upload decoded image to gpu - use GL_RGBA not GL_RGBA8

        stbi_image_free(decodedImage); // frees from cpu memory
        textureId = id;
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
