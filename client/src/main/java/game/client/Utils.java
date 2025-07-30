package game.client;

import static org.lwjgl.opengl.GL20.glGetUniformfv;

// just debug stuff. nothing here is used in the final build
public class Utils {
    public static void printMatrixFromUniform(String name, int uniform){
        float[] debug = new float[16];
        System.out.println(name + " uniform location: " + uniform);
        glGetUniformfv(Opengl.vertex_program, uniform, debug);
        // Correct: interpret column-major layout as row-major
        for (int row = 0; row < 4; row++) {
            System.out.printf("[");
            for (int col = 0; col < 4; col++) {
                System.out.printf(" %8.3f", debug[col * 4 + row]); // ← key fix
            }
            System.out.println(" ]");
        }
    }
}
