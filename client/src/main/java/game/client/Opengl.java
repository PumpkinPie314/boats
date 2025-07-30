package game.client;

import java.io.IOException;
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
import static org.lwjgl.opengl.GL41.glProgramUniformMatrix4fv;
import static org.lwjgl.opengl.GL41.glUseProgramStages;

public class Opengl {
    static int vertex_program;
    static int fragment_program;
    static int pipeline;

    static int modelMatrix;     
    static int viewMatrix;      
    static int projectionMatrix;
    static int drawScreenSpace;
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
        drawScreenSpace = glGetUniformLocation(vertex_program, "drawScreenSpace"); 
        modelMatrix = glGetUniformLocation(vertex_program, "modelMatrix"); 
        viewMatrix = glGetUniformLocation(vertex_program, "viewMatrix");
        projectionMatrix = glGetUniformLocation(vertex_program, "projectionMatrix");
    }
    public static int ShaderFromResource(int type, String name) {
        try {
            String code = new String(Main.class.getResourceAsStream(name).readAllBytes());
            return GL41.glCreateShaderProgramv(type, code);
        } catch (IOException e) {
            System.err.println("cannot read shader files: "+e);
            System.exit(1);
            return -1;//unreachable
        }
    }

    public static void setDrawScreenSpace(boolean value) {
        GL41.glProgramUniform1i(
            vertex_program,
            drawScreenSpace,
            value ? 1 : 0
        );
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
