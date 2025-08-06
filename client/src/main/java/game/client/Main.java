package game.client;
import java.io.ObjectOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import game.common.Boat;
import game.common.Config;
import game.common.GameState;
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

import static org.lwjgl.opengl.GL45.glBindTextureUnit;
@SuppressWarnings("BusyWait") // allows sleep() in a loop
public class Main {
    public static final String WINDOW_NAME = "Game";
    public static final String CONFIG_FILE = "client-config.txt";
    public static GameState gameState = new GameState();
    public static Socket socket;
    public static ObjectOutputStream out;
    public static ObjectInputStream in;
    public static Random rand = new Random();
    public static void main(String[] args) throws InterruptedException , IOException {
        System.out.println("hello from client!");

        System.out.println("loading config");
        File config_file = new File(CONFIG_FILE);
        if (!config_file.exists()) {
            System.out.println("no config found...");
            ClientConfig.createDefault();
            System.out.println("default config created. make sure to change the ip and port if you are playing online!");
            System.exit(1);
        }
        ClientConfig.load(CONFIG_FILE);
        
        System.out.println("making the window...");
        Window.init();
        
        System.out.println("loading shaders...");
        Opengl.init();
        
        
        System.out.println("loading textures...");
        Opengl.loadTexture("/textures/textures.png");


        Camera.setProjection(-10.0f, 10.0f, -10.0f, 10.0f, 100.0f);
        
        // connect to server
        System.out.println("connecting to server ip:" + ClientConfig.serverip + " on port: "+ ClientConfig.serverport+ "...");
        try {
            socket = new Socket();
            socket.setTcpNoDelay(true);
            socket.connect(new InetSocketAddress(ClientConfig.serverip, ClientConfig.serverport), ClientConfig.connectiontimout);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            System.out.println("connected!");
        } catch (IOException e) {
            System.err.println("Failed to connect to server: " + e.getMessage());
            cleanup();
            System.exit(1);
        }
        
        System.out.println("game started!");
        Boat myboat = new Boat();
        myboat.sectionHealth = new int[] {3,3,3,1,2,3};
        int myboat_index = -1; //the index of this client's boat in the gamestate.boats arraylist
        List<Vector3f> wakeFoam = new ArrayList<>();
        while (!glfwWindowShouldClose(Window.id)) {
            long start_time = System.nanoTime();
            
            // parse incoming game state from server
            try {
                myboat_index = in.readInt();
                gameState = (GameState) in.readObject();
            } catch (ClassNotFoundException  e) {
                System.err.println("failed parsing gamestate packet");
                System.exit(1);
            } catch (IOException e) {
                System.out.println("server disconnected!");
                try { socket.close(); } catch (IOException ignored) {}
                cleanup();
                System.exit(0);
            }
            if (myboat_index == -1) {System.err.println("by boat index not set from server!");} 
            
            // position camera
            Camera.setPositionView(
                new Vector3f(myboat.position).add(0, 2, 0), 
                new Quaternionf().lookAlong(new Vector3f(0,-1,0), new Vector3f(0,0,1))
            );

            

            // collect inputs
            glfwPollEvents();
            Config config = gameState.config;
            float dt = 1f/config.fps;

            if (GLFW_PRESS == glfwGetKey(Window.id, ClientConfig.saildown)) myboat.mast_down_percent += config.mast_drop_speed * dt;
            if (GLFW_PRESS == glfwGetKey(Window.id, ClientConfig.sailup)) myboat.mast_down_percent -= config.mast_raise_speed * dt;
            if (GLFW_PRESS == glfwGetKey(Window.id, ClientConfig.wheelleft)) myboat.wheel_turn_percent -= config.wheel_turn_speed * dt;
            if (GLFW_PRESS == glfwGetKey(Window.id, ClientConfig.wheelright)) myboat.wheel_turn_percent += config.wheel_turn_speed * dt;
            if (GLFW_PRESS == glfwGetKey(Window.id, ClientConfig.sailleft)) myboat.sail_turn_percent -= config.sail_turn_speed * dt;
            if (GLFW_PRESS == glfwGetKey(Window.id, ClientConfig.sailright)) myboat.sail_turn_percent += config.sail_turn_speed * dt;

            // update boat
            if (myboat.mast_down_percent > 1f) myboat.mast_down_percent = 1f;
            if (myboat.mast_down_percent < 0f) myboat.mast_down_percent = 0f;
            if (myboat.wheel_turn_percent > 1f) myboat.wheel_turn_percent = 1f;
            if (myboat.wheel_turn_percent < -1f) myboat.wheel_turn_percent = -1f;
            if (myboat.sail_turn_percent > 1f) myboat.sail_turn_percent = 1f;
            if (myboat.sail_turn_percent < -1f) myboat.sail_turn_percent = -1f;
            myboat.cannonRotation = new Quaternionf();
                //snap to straight 
            if (GLFW_PRESS != glfwGetKey(Window.id, ClientConfig.saildown) &&
                GLFW_PRESS != glfwGetKey(Window.id, ClientConfig.sailup) && 
                GLFW_PRESS != glfwGetKey(Window.id, ClientConfig.wheelleft) && 
                GLFW_PRESS != glfwGetKey(Window.id, ClientConfig.wheelright) && 
                GLFW_PRESS != glfwGetKey(Window.id, ClientConfig.sailleft) && 
                GLFW_PRESS != glfwGetKey(Window.id, ClientConfig.sailright)
            ){
                if (Math.abs(myboat.wheel_turn_percent) < 5 * config.wheel_turn_speed * dt) myboat.wheel_turn_percent = 0;
                if (Math.abs(myboat.sail_turn_percent) < 5 * config.sail_turn_speed * dt) myboat.sail_turn_percent = 0;
            }



            Vector3f forward = new Vector3f(0,0,1).rotate(myboat.rotation);
            // gameState.wind_direction = myboat.rotation; // temp!
            myboat.velocity = forward.mul(myboat.mast_down_percent * config.sail_speed);
            myboat.rotation.integrate(dt, 0, myboat.wheel_turn_percent * config.turn_speed * -1,0);

            myboat.position.add(myboat.velocity.mul(dt));


            // send new updated boat
            out.reset();
            out.writeObject(myboat);
            out.flush();
            
            // draw
            glClearColor(0.0f, 0.5f,0.5f,1.0f);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            {
                float[] vertex_data = {
                    // x y z, r g b, s t
                    5, 0, 5, 1, 1, 1, 0, 0,
                    0, 0, 5, 1, 1 ,1, 1, 0,
                    5, 0, 0, 1, 1, 1, 0, 1,
                    0, 0, 0, 1, 1 ,1, 1, 1,
                };
                int[] indices = {
                    0, 1, 3, 
                    0, 2, 3,
                };
                
                Mesh quad = new Mesh(vertex_data, indices);
                quad.draw(new Matrix4f());
            }
            {
                // all wake foam logic
                // for (Boat boat : gameState.boats) {
                //     wakeFoam.add(boat.position);
                // }

            }
            {
                // draw boats
                Drawer.drawhHull(myboat);
                for (int i = 0; i < gameState.boats.size(); i++) {
                    if (i == myboat_index) continue;// the client can draw her own boat
                    Drawer.drawhHull(gameState.boats.get(i));
                }
            } 
            glfwSwapBuffers(Window.id);


            long delta_ns = System.nanoTime() - start_time;
            double delta_ms = delta_ns / 1_000_000.0;
            double frametime = delta_ms / 1_000.0;
            glfwSetWindowTitle(Window.id, WINDOW_NAME + "   " + "frametime: " + (int)delta_ms);
        }
        cleanup();
    }
    public static void cleanup() throws IOException{
        Mesh.cleanupAll();
        Opengl.cleanup();
        Window.cleanUp();
    }
}
