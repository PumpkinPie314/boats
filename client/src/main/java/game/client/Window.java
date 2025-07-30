package game.client;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Window {
    public long id;
    public Window() {
        System.out.println("starting window");
        if (!glfwInit()){
            throw new IllegalStateException("Unable to initialize GLFW");
        }
        id = glfwCreateWindow(480*2, 480*2, Main.WINDOW_NAME, NULL, NULL);
        if (id == 0.0) {
            glfwTerminate();
            System.err.println("");
            System.exit(1); 
        }
        glfwMakeContextCurrent(id);
    }
    public void cleanUp() {
        glfwDestroyWindow(id);
        glfwTerminate();
    }
}
