package game.client;
import java.io.ObjectOutputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import game.common.Boat;
import game.common.GameState;
import game.common.InputState;
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
    public static GameState gameState = new GameState();
    public static Socket socket;
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

        socket = new Socket();
        socket.setTcpNoDelay(true);
        try {
            socket.connect(new InetSocketAddress(Config.serverip, Config.serverport), Config.connectiontimout);
        } catch (IOException e) {
            System.err.println("could not connect to server. aborting.");
            System.exit(1);
        }
        System.out.println("connected!");
        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
        
        System.out.println("making the window...");
        Window.init();
        
        System.out.println("loading shaders...");
        Opengl.init();
        
        Camera.setProjection(-40.0f, 40.0f, -40.0f, 40.0f, 100.0f);
        
        System.out.println("game started!");
        final int target_fps = 20; // slow fps because it is online
        while (!glfwWindowShouldClose(Window.id)) {
            long start_time = System.nanoTime();
            // clear
            glClearColor(0.0f, 0.5f,0.5f,1.0f);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            // parse incoming game state from server
            int myboat_index = in.readInt();
            System.out.println("myboat: " + myboat_index);
            // int packet_size = in.readInt();
            // byte[] gamestate_packet = new byte[packet_size];
            // in.readFully(gamestate_packet);
            // ObjectInputStream objectIn = new ObjectInputStream(new ByteArrayInputStream(gamestate_packet));
            try {
                gameState = (GameState) in.readObject();
                System.out.println("gamestate updated. x = " + (gameState.boats.isEmpty() ? "gamestate empty" : gameState.boats.get(0).position.x));
            } catch (ClassNotFoundException | IOException e) {
                System.err.println("failed parsing gamestate packet");
                System.exit(1);
            }
            Boat myboat = gameState.boats.get(myboat_index);
            
            // position camera
            Camera.setPositionView(
                new Vector3f(0,0, 10), // height(z) doesn't matter for orthographic projections
                new Quaternionf().lookAlong(new Vector3f(0,-1,0), new Vector3f(0,0,-1))
            );


            
            // draw
            gameState.boats.stream().forEach(Drawer::draw);
            glfwSwapBuffers(Window.id);


            // collect inputs
            glfwPollEvents();
            InputState inputPacket = new InputState();
            inputPacket.saildown = GLFW_PRESS == glfwGetKey(Window.id, Config.saildown);
            inputPacket.sailup  = GLFW_PRESS == glfwGetKey(Window.id, Config.sailup);
            inputPacket.wheelleft = GLFW_PRESS == glfwGetKey(Window.id, Config.wheelleft);
            inputPacket.wheelright = GLFW_PRESS == glfwGetKey(Window.id, Config.wheelright);
            inputPacket.sailleft = GLFW_PRESS == glfwGetKey(Window.id, Config.sailleft);
            inputPacket.sailright = GLFW_PRESS == glfwGetKey(Window.id, Config.sailright);
            inputPacket.cannon_rotation = new Quaternionf();
            // send inputs;
            out.writeObject(inputPacket);
            out.flush();

            long delta_ns = System.nanoTime() - start_time;
            double delta_ms = delta_ns / 1_000_000.0;
            double fps = 1000.0 / delta_ms;
            if (fps > target_fps) {
                Thread.sleep(1000L/target_fps - (long)delta_ms);
                fps = target_fps;
            }
            glfwSetWindowTitle(Window.id, WINDOW_NAME + "boat screen pos: " + Opengl.getScreenSpace(myboat.position).x() + "," +  Opengl.getScreenSpace(myboat.position).x() + "    fps: " + (int)fps);
        }
        cleanup();
    }
    public static void cleanup() throws IOException{
        Mesh.cleanupAll();
        Opengl.cleanup();
        Window.cleanUp();
        socket.close();
    }
}
