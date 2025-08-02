package game.client;
import java.io.ObjectOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;

import org.joml.Matrix4f;
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
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_LINES;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL11.glDrawElements;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL20.glGetAttribLocation;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL45.glCreateBuffers;
import static org.lwjgl.opengl.GL45.glCreateVertexArrays;
import static org.lwjgl.opengl.GL45.glEnableVertexArrayAttrib;
import static org.lwjgl.opengl.GL45.glNamedBufferData;
import static org.lwjgl.opengl.GL45.glVertexArrayAttribBinding;
import static org.lwjgl.opengl.GL45.glVertexArrayAttribFormat;
import static org.lwjgl.opengl.GL45.glVertexArrayElementBuffer;
import static org.lwjgl.opengl.GL45.glVertexArrayVertexBuffer;
@SuppressWarnings("BusyWait") // allows sleep() in a loop
public class Main {
    public static final String WINDOW_NAME = "Game";
    public static final String CONFIG_FILE = "client-config.txt";
    public static GameState gameState = new GameState();
    public static boolean connected_to_server = false;
    public static Socket socket;
    public static ObjectOutputStream out;
    public static ObjectInputStream in;
    public static void main(String[] args) throws InterruptedException , IOException {
        System.out.println("hello from client!");

        System.out.println("loading config");
        File config_file = new File(CONFIG_FILE);
        if (!config_file.exists()) {
            Config.createDefault();
            System.out.println("default config created");
        }
        Config.load(CONFIG_FILE);
        
        System.out.println("making the window...");
        Window.init();
        
        System.out.println("loading shaders...");
        Opengl.init();
        
        Camera.setProjection(-10.0f, 10.0f, -10.0f, 10.0f, 100.0f);
        
        System.out.println("game started!");
        Boat myboat = new Boat();// this new boat is not in gamestate and is overwriten immidietly
        while (!glfwWindowShouldClose(Window.id)) {
            long start_time = System.nanoTime();
            // clear
            glClearColor(0.0f, 0.5f,0.5f,1.0f);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            // connect to server
            if (!connected_to_server) {
                glfwPollEvents();
                glfwSetWindowTitle(Window.id, "connecting...");
                if(connectToServer()){
                    System.out.println("connecting to server ip:" + Config.serverip + " on port: "+ Config.serverport+ "...");
                    connected_to_server = true;
                    System.out.println("connected!");
                } else {
                    socket.close();
                    Thread.sleep(1000);
                    continue;
                }
            }
            
            // parse incoming game state from server
            try {
                int myboat_index = in.readInt();
                gameState = (GameState) in.readObject();
                myboat = gameState.boats.get(myboat_index);
            } catch (ClassNotFoundException  e) {
                System.err.println("failed parsing gamestate packet");
                System.exit(1);
            } catch (IOException e) {
                System.out.println("server disconnected!");
                connected_to_server = false;
                continue;
            }
            
            // position camera
            Camera.setPositionView(
                new Vector3f(myboat.position).add(0, 1, 0), 
                new Quaternionf().lookAlong(new Vector3f(0,-1,0), new Vector3f(0,0,1))
            );
            // draw line for testing
            {
                float[] vertex_data = {
                    // x y z, r g b
                    0, 0, 0, 0, 0, 0,
                    5, 0, 5, 0, 0 ,0,
                };
                int[] indices = {
                    0, 1
                };
                
                Mesh line = new Mesh(vertex_data, indices);

                Opengl.setDrawScreenSpace(false);
                Opengl.setModelMatrix(new Matrix4f());
                glBindVertexArray(line.vao);
                glDrawElements(GL_LINES, line.size , GL_UNSIGNED_INT, 0);
            }


            
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
            glfwSetWindowTitle(Window.id, WINDOW_NAME + "   fps: " + (int)fps);
        }
        cleanup();
    }
    public static boolean connectToServer() {
        try {
            socket = new Socket();
            socket.setTcpNoDelay(true);
            socket.connect(new InetSocketAddress(Config.serverip, Config.serverport), Config.connectiontimout);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            return true;
        } catch (IOException e) {
            return false;
        }
    }
    public static void cleanup() throws IOException{
        Mesh.cleanupAll();
        Opengl.cleanup();
        Window.cleanUp();
    }
}
