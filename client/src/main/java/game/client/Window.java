package game.client;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwShowWindow;
import static org.lwjgl.glfw.GLFW.glfwSwapInterval;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Window {
    public static long id;
    public static int width = 960;
    public static int height = 960;
    public static void init() {
        if (!glfwInit()){
            throw new IllegalStateException("Unable to initialize GLFW");
        }
        id = glfwCreateWindow(width, height, Main.WINDOW_NAME, NULL, NULL);
        if (id == 0.0) {
            glfwTerminate();
            System.err.println("");
            System.exit(1); 
        }
        glfwMakeContextCurrent(id);
        glfwSwapInterval(1);
        glfwShowWindow(id);
    }
    public static void cleanUp() {
        glfwDestroyWindow(id);
        glfwTerminate();
    }
}
