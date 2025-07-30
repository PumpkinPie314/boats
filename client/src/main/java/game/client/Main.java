package game.client;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Scanner;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_A;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_D;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.glfwGetKey;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSetWindowTitle;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
@SuppressWarnings("BusyWait") // allows sleep() in a loop
public class Main {
    public static final String WINDOW_NAME = "Game";
    public static final String CONFIG_FILE = "client-config.txt";
    public Camera camera;
    public static void main(String[] args) throws InterruptedException , IOException {
        System.out.println("hello from client!");

        System.out.println("loading config");
        File config_file = new File(CONFIG_FILE);
        if (!config_file.exists()) {
            Config.createDefault();
            System.out.println("default config created");
        }
        Config.load(CONFIG_FILE);
        
        System.out.println("connecting to server ip:" + Config.serverip + " on port: "+ Config.serverport+ "...");

        Socket socket = new Socket(); // Create an unconnected socket
        try {
            socket.connect(new InetSocketAddress(Config.serverip, Config.serverport), Config.connectiontimout); // Attempt connection with timeout
        } catch (IOException e) {
            System.err.println("could not connect to server. aborting.");
            System.exit(1);
        }
        System.out.println("connected!");
        DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        boolean clientLoop = true;
        Scanner input = new Scanner(System.in);
        out.writeUTF("this is a test!");
        out.flush();
        input.close();
        socket.close();
        // System.exit(0);
        
        
        
        
        
        
        
        System.out.println("making the window...");
        Window.init();

        System.out.println("loading shaders...");
        Opengl.init();

        System.out.println("game started!");
        Camera.setProjection(-40.0f, 40.0f, -40.0f, 40.0f, 100.0f);
        Camera.setPositionView(
            new Vector3f(0,0, 10), // height(z) doesn't matter for orthographic projections
            new Quaternionf().lookAlong(new Vector3f(0,-1,0), new Vector3f(0,0,-1))
        );
            
        Boat myboat = new Boat();
        myboat.sectionDamage = new int[] {0,1,2,3,4,5};

        final int target_fps = 20; // slow fps because it is online
        while (!glfwWindowShouldClose(Window.id)) {
            long start_time = System.nanoTime();

            glClearColor(0.0f, 0.5f,0.5f,1.0f);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            myboat.draw();
            // printMatrixFromUniform("model", Opengl.modelMatrix);

            /* Swap front and back buffers */
            glfwSwapBuffers(Window.id);

            /* Poll for and process events */
            glfwPollEvents();

            if (GLFW_PRESS == glfwGetKey(Window.id, Config.saildown));

            long delta_ns = System.nanoTime() - start_time;
            double delta_ms = delta_ns / 1_000_000.0;
            double fps = 1000.0 / delta_ms;
            if (fps > target_fps) {
                Thread.sleep(1000L/target_fps - (long)delta_ms);
                fps = target_fps;
            }
            glfwSetWindowTitle(Window.id, WINDOW_NAME + "boat screen pos: " + Opengl.getScreenSpace(myboat.position).x() + "," +  Opengl.getScreenSpace(myboat.position).x() + "    fps: " + (int)fps);
        }

    }
    public static void cleanup(){
        Mesh.cleanupAll();
        Opengl.cleanup();
        Window.cleanUp();
    }
}
