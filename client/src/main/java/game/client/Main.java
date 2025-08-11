package game.client;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.stream.IntStream;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import game.common.Boat;
import game.common.Config;
import game.common.DamageEvent;
import game.common.FireEvent;
import game.common.GameState;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.glfwGetCursorPos;
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
    public static ObjectOutputStream out;
    public static ObjectInputStream in;
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

        System.out.println("making the window...");
        Window.init();
        
        System.out.println("loading shaders...");
        Opengl.init();
        
        System.out.println("loading textures...");
        Opengl.loadTexture("/textures/textures.png");

        // mesh settup
        Drawer.generateMeshes();

        // camera frustum
        Opengl.projectionMatrix.setOrtho(-10.0f, 10.0f, -10.0f, 10.0f, -100.0f, 100.0f);
        // Opengl.projectionMatrix.setOrtho(-3.0f, 3.0f, -3.0f, 3f, -100.0f, 100.0f);

        System.out.println("game started!");
        Boat myboat = new Boat();
        FireEvent myLastFired = null;
        DamageEvent myLastHit = null;
        int myboat_index = -1; //the index of this client's boat in the gamestate.boats arraylist
        Config last_config = new Config();
        Deque<CannonBall> cannonBalls = new ArrayDeque<>();
        long cannonballs_created_up_to_tick = 0;
        long damage_dealt_up_to_tick = 0;

        List<Deque<float[]>> wakes = new ArrayList<>(new ArrayDeque<>());
        Deque<float[]> floam = new ArrayDeque<>();
        while (!glfwWindowShouldClose(Window.id)) {
            long start_time = System.nanoTime();
            Opengl.updateMatrixUniforms();
            Config config;
            { // parse incoming game state from server
                try {
                    myboat_index = in.readInt();
                    gameState = (GameState) in.readObject();
                    /* 
                    * the game speed is throttled here. 
                    * the thread is blocked while it waits for the next packet
                    * the server controls client fps
                    */
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
                config = gameState.config;
                for (Field field : config.getClass().getFields()) {
                    try {
                        if (!field.get(last_config).equals(field.get(config))){
                            System.out.println("config feild: " + field.getName() + " changed from: " + field.get(last_config) +" to: " + field.get(config));
                            field.set(last_config, field.get(config));
                        }
                    } catch (IllegalArgumentException | IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
            float dt = 1f/config.fps;
            Vector3f mouse_screen_space = new Vector3f();
            { // collect inputs
                glfwPollEvents();

                if (GLFW_PRESS == glfwGetKey(Window.id, ClientConfig.saildown)) myboat.mast_down_percent += config.mast_drop_speed * dt;
                if (GLFW_PRESS == glfwGetKey(Window.id, ClientConfig.sailup)) myboat.mast_down_percent -= config.mast_raise_speed * dt;
                if (GLFW_PRESS == glfwGetKey(Window.id, ClientConfig.wheelleft)) myboat.wheel_turn_percent -= config.wheel_turn_speed * dt;
                if (GLFW_PRESS == glfwGetKey(Window.id, ClientConfig.wheelright)) myboat.wheel_turn_percent += config.wheel_turn_speed * dt;
                if (GLFW_PRESS == glfwGetKey(Window.id, ClientConfig.sailleft)) myboat.sail_turn_percent -= config.sail_turn_speed * dt;
                if (GLFW_PRESS == glfwGetKey(Window.id, ClientConfig.sailright)) myboat.sail_turn_percent += config.sail_turn_speed * dt;
                { /* get mouse inputs */
                    double[] mouse_x_buff = new double[1];
                    double[] mouse_y_buff = new double[1];
                    glfwGetCursorPos(Window.id, mouse_x_buff, mouse_y_buff);
                    float mx = (float) mouse_x_buff[0];
                    float my = (float) mouse_y_buff[0];
                    mx = ((mx/Window.width)-0.5f)*2;
                    my = -((my/Window.height)-0.5f)*2;
                    mouse_screen_space = new Vector3f(mx, 0, my);
                }
                // aiming cannon angle
                float tau = (float) Math.TAU;
                float cannon_angle = mouse_screen_space.x*(-1f/2)*tau; // forwards
                if (cannon_angle > 0          && cannon_angle <  (1f/4)*tau - config.cannon_angle_limit) cannon_angle =  (1f/4)*tau - config.cannon_angle_limit; // front left
                if (cannon_angle < 0          && cannon_angle > -(1f/4)*tau + config.cannon_angle_limit) cannon_angle = -(1f/4)*tau + config.cannon_angle_limit; // front right
                if (cannon_angle < (1f/2)*tau && cannon_angle >  (1f/4)*tau + config.cannon_angle_limit) cannon_angle =  (1f/4)*tau + config.cannon_angle_limit; // back left
                if (cannon_angle >-(1f/2)*tau && cannon_angle < -(1f/4)*tau - config.cannon_angle_limit) cannon_angle = -(1f/4)*tau - config.cannon_angle_limit; // back right
                
                float cannon_height = (mouse_screen_space.z-1)*(1f/8)*tau ; // -1,1 -> 0,(1/8)tau
                myboat.cannonRotation = new Quaternionf(myboat.rotation)
                    .rotateAxis(cannon_angle, new Vector3f(0,1,0))
                    .rotateAxis(cannon_height, new Vector3f(1,0,0));
            }
            { // update boat
                if (myboat.mast_down_percent > 1f) myboat.mast_down_percent = 1f;
                if (myboat.mast_down_percent < 0f) myboat.mast_down_percent = 0f;
                if (myboat.wheel_turn_percent > 1f) myboat.wheel_turn_percent = 1f;
                if (myboat.wheel_turn_percent < -1f) myboat.wheel_turn_percent = -1f;
                if (myboat.sail_turn_percent > 1f) myboat.sail_turn_percent = 1f;
                if (myboat.sail_turn_percent < -1f) myboat.sail_turn_percent = -1f;
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
            }
            { // boat movement
                Vector3f saildir = new Vector3f(0,0,1).rotate(new Quaternionf(myboat.rotation)).rotateY(myboat.sail_turn_percent * Main.gameState.config.sail_angle_limit * -1);
                Vector3f winddir = new Vector3f(gameState.wind);
                Vector3f forward = new Vector3f(0,0,1).rotate(myboat.rotation);
                float sail_efficiency_percent = (saildir.dot(winddir)+1)/2; // you can find this in drawer.drawBoat sail!
                float max = config.sail_speed_with_wind;
                float min = config.sail_speed_no_wind;
                float speed = min + (max-min) * sail_efficiency_percent;

                myboat.velocity = new Vector3f(forward).mul(myboat.mast_down_percent*speed);
                myboat.rotation.integrate(dt, 0, myboat.wheel_turn_percent * config.turn_speed * -1,0);

                myboat.position.add(new Vector3f(myboat.velocity).mul(dt));
            }
            { // shooting 
                if (
                    GLFW_PRESS == glfwGetKey(Window.id, GLFW_KEY_SPACE)
                    && (
                        myLastFired == null ||
                        gameState.tick_current - myLastFired.tick_fired > (1/dt) * config.cannon_fire_cooldown
                    )
                ) {
                    myLastFired = new FireEvent();
                    myLastFired.tick_fired = gameState.tick_current;
                    myLastFired.position = new Vector3f(myboat.position);
                    myLastFired.velocity = new Vector3f(0,0,1)
                        .rotate(new Quaternionf(myboat.cannonRotation))
                        .mul(config.cannon_speed)
                        .add(new Vector3f(myboat.velocity));
                    
                }
            }
            { // create connonballs
                if (!cannonBalls.isEmpty()) cannonballs_created_up_to_tick = cannonBalls.peekFirst().tick_fired;
                for (int i = 0; i < gameState.fireEvents.size(); i++) {
                    FireEvent fireEvent = gameState.fireEvents.get(i);
                    if (fireEvent == null) continue; 
                    if (fireEvent.tick_fired > cannonballs_created_up_to_tick){
                        CannonBall cb = new CannonBall();
                        cb.owner_boat_id = i;
                        cb.tick_fired = fireEvent.tick_fired;
                        cb.position = new Vector3f(fireEvent.position);
                        cb.velocity = new Vector3f(fireEvent.velocity);
                        // fast forward
                        int missed_tick_count = gameState.tick_current - fireEvent.tick_fired;
                        for (int _i = 0; _i < missed_tick_count  ; _i++) {
                            cb.velocity.add(new Vector3f(0,-1,0).mul(config.gravity * dt));
                            cb.position.add(new Vector3f(cb.velocity).mul(dt));
                        }
                        cannonBalls.addFirst(cb);
                            /*
                            * this is backwards to how you would normally use a queue: adding to the front and removing from the back.
                            * it is this way because removed cannonballs are more likely to be older. 
                            * this way the array only shifts cannonballs older then it, which is usually a lot less
                            */
                    };
                }
            }
            { // update cannonballs
            
                /*
                * the client owns there own cannonballs. 
                * after shooting, it sends just the FireEvent, and all the other clients reconstruct the ball for themselves
                * hits are determined by the client that shot the ball. this garentees: if you see it hit on your screen, then it hits.
                * this way you dont have to aim for where your opponent is on the server, which could be ahead of where you see them.
                * this is called favor the shooter
                */
                float[][] segment_hurt_sphere_centers = {
                    // portside                // starboard
                    // x,y,z,radius            // x,y,z,radius 
                    {-1f/4, 0,  1f/2, 1f/3, }, { 1f/4, 0,  1f/2, 1f/3}, //uv  // bow
                    {-1f/4, 0,  0f/2, 1f/3}, { 1f/4, 0,  0f/2, 1f/3}, //wx
                    {-1f/4, 0, -1f/2, 1f/3}, { 1f/4, 0, -1f/2, 1f/3}, //yz  // stern
                };
                float hurt_radius = 1f/3;
                Iterator<CannonBall> ballIter = cannonBalls.iterator();
                while (ballIter.hasNext()) {
                    CannonBall cb = ballIter.next();
                    cb.velocity.add(new Vector3f(0,-1,0).mul(config.gravity * dt));
                    cb.position.add(new Vector3f(cb.velocity).mul(dt));

                    if (cb.position.y < -0.1) {
                        ballIter.remove();
                        continue;
                    }

                    // hit other boats
                    if (cb.owner_boat_id!=myboat_index) continue; // ignore balls not shot by me
                    for (int boat_index = 0; boat_index < gameState.boats.size(); boat_index++) {
                        if (boat_index == myboat_index) continue; // no self damage
                        Boat boat = gameState.boats.get(boat_index);
                        for (int i = 0; i < segment_hurt_sphere_centers.length; i++) {
                            if (boat.sectionHealth[i] < 1) continue; 
                            Vector3f hurtSphere = new Vector3f(segment_hurt_sphere_centers[i])
                                .rotate(boat.rotation)
                                .add(boat.position);
                            if (hurtSphere.distance(cb.position) < hurt_radius){
                                myLastHit = new DamageEvent();
                                myLastHit.tick_hit = gameState.tick_current;
                                myLastHit.boatid = boat_index;
                                myLastHit.sectionid = i;
                                ballIter.remove();
                                break;
                            }
                        }
                    }
                }
            }
            { // taking damage
                float hurt_radius = 1f/3;
                float[][] segment_hurt_sphere_centers = {
                    // portside                // starboard
                    // x,y,z,radius            // x,y,z,radius 
                    {-1f/4, 0,  1f/2, 1f/3}, { 1f/4, 0,  1f/2, 1f/3}, //uv  // bow
                    {-1f/4, 0,  0f/2, 1f/3}, { 1f/4, 0,  0f/2, 1f/3}, //wx
                    {-1f/4, 0, -1f/2, 1f/3}, { 1f/4, 0, -1f/2, 1f/3}, //yz  // stern
                };
                for (DamageEvent hit : gameState.damageEvents) {
                    if (hit == null) continue;
                    if (hit.boatid != myboat_index) continue;
                    if (hit.tick_hit > damage_dealt_up_to_tick) {
                        damage_dealt_up_to_tick = hit.tick_hit;
                        myboat.sectionHealth[hit.sectionid] -= 1;
                    }
                    // remove the ball that just hit me
                    Vector3f hurtSphere = new Vector3f(segment_hurt_sphere_centers[hit.sectionid])
                        .rotate(myboat.rotation)
                        .add(myboat.position);
                    cannonBalls.removeIf(cb-> hurtSphere.distance(cb.position) < hurt_radius);
                }
            }
            { // dath and respawning
                if (myboat.sectionHealth.equals(new int[] {0,0,0,0,0,0})) myboat = new Boat();
            }

            { // send new updates to server
                out.reset();
                out.writeObject(myLastFired);
                out.writeObject(myLastHit);
                out.writeObject(myboat);
                out.flush();
            }
            // draw
            long render_start_time = System.nanoTime();
            glClearColor(0.0f, 0.5f,0.5f,1.0f);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            { // draw temp quad
                float[] vertex_data = {
                    // x y z, r g b, s t
                    10, -0.001f, 8, 1, 1,
                    0, -0.001f, 8, 0, 1,
                    10, -0.001f, 0, 1, 0,
                    0, -0.001f, 0, 0, 0,
                };
                int[] indices = {
                    0, 1, 3, 
                    0, 2, 3,
                };
                
                Mesh quad = new Mesh(vertex_data, indices);
                quad.draw(new Matrix4f());
                quad.cleanup();
            }
            { // draw temp boat meshes
                IntStream.range(0, 6).forEach(i -> Drawer.hullMeshes.get(i).draw(new Matrix4f().translate(5, 0, -5.0f)));
                IntStream.range(6, 12).forEach(i -> Drawer.hullMeshes.get(i).draw(new Matrix4f().translate(0, 0, -5.0f)));
                IntStream.range(12, 18).forEach(i -> Drawer.hullMeshes.get(i).draw(new Matrix4f().translate(-5, 0, -5.0f)));
            }
            
            int boat_pixel_length = 128;
            float bw = (float) boat_pixel_length / Opengl.atlasWidth;
            float bh = (float) boat_pixel_length / Opengl.atlasHeight;
            { // draw wake          
                float wake_drift = 0.2f;
                while (wakes.size() < gameState.boats.size()) {
                    wakes.add(new ArrayDeque<>());
                }
                for (int i = 0; i < gameState.boats.size(); i++) {
                    Boat boat = gameState.boats.get(i);
                    Deque<float[]> wake = wakes.get(i);

                    if (wake.peekFirst() == null || new Vector3f(wake.peekFirst()).distance(boat.position) > 1f/4) {
                        Vector3f leftVel = new Vector3f(-wake_drift, -0.01f, 0 ).rotate(boat.rotation);
                        Vector3f rightVel = new Vector3f(wake_drift, -0.01f, 0 ).rotate(boat.rotation);
                        Vector3f leftPos = new Vector3f(-1f/4, 0, 1f/2 ).rotate(boat.rotation).add(boat.position);
                        Vector3f rightPos = new Vector3f(1f/4, 0, 1f/2 ).rotate(boat.rotation).add(boat.position);
                        float[] left_vert = new float[] {leftPos.x, leftPos.y, leftPos.z, (4f/2) * bw, bh,
                            leftVel.x, leftVel.y, leftVel.z};
                        float[] right_vert = new float[] {rightPos.x, rightPos.y, rightPos.z, (5f/2) * bw, bh,
                            rightVel.x, rightVel.y, rightVel.z};
                        wake.addFirst(left_vert);
                        wake.addFirst(right_vert);
                    }
                    wake.stream().forEach(vert -> {
                        // drift each vertex
                        vert[0] += vert[5] * dt;
                        vert[1] += vert[6] * dt;
                        vert[2] += vert[7] * dt;
                        // fade
                        vert[4] += dt/config.wake_length;
                    });
                    wake.removeIf(vert -> vert[4] > 2*bh);

                    float[] vertex_data = new float[wake.size() * 8];
                    int pointer = 0;
                    for (float[] vert : wake) {
                        float[] vertex = new float[] {vert[0],vert[1],vert[2],vert[3],vert[4]};
                        System.arraycopy(vertex, 0, vertex_data, pointer, 5);
                        pointer += 5;
                    }
                    int[] indices = IntStream.range(0, wake.size()).toArray();
                    Mesh wakeMesh = new Mesh(vertex_data, indices);
                    wakeMesh.drawStrip(new Matrix4f().translate(new Vector3f(0,-0.01f,0)));
                    wakeMesh.cleanup();
                }
            }
            { // draw foam
                
            }

            { // draw boats
                Drawer.drawBoat(myboat);
                for (int i = 0; i < gameState.boats.size(); i++) {
                    if (i == myboat_index) continue;
                    Drawer.drawBoat(gameState.boats.get(i));
                }
            }
            { // draw cannonballs
                cannonBalls.stream().forEach(Drawer::drawCannonBall);
            }
            { // draw wheel ui thingy
                float[] quad = {
                    -1f/4, 2, 0, (7f/4)*bw , (2f/2)*bh, 
                     1f/4, 2, 0, (6f/4)*bw , (2f/2)*bh,
                    -1f/4, 0, 0, (7f/4)*bw , (4f/2)*bh,
                     1f/4, 0, 0, (6f/4)*bw , (4f/2)*bh,
                };
                int[] indices = {0, 1, 3, 0, 2, 3};
                Mesh wheelMeshUI = new Mesh(quad, indices);
                wheelMeshUI.draw(new Matrix4f()
                    .mul(new Matrix4f(Opengl.viewMatrix).invert())
                    .mul(new Matrix4f(Opengl.projectionMatrix).invert())
                    .translate(myboat.wheel_turn_percent, -1, 0)
                    .scale(1/32f)
                );
                wheelMeshUI.cleanup();
            }

            { // position camera
                float tau = (float) Math.TAU;
                float yaw = (-mouse_screen_space.x-1f)*(1f/2)*tau; // -1,1 -> 0,-tau
                float pitch = (mouse_screen_space.z-3)*(1f/16)*tau; // -1,1 -> (1/8)tau,(1/4)tau   aka 45°,90°
                Opengl.viewMatrix.identity()
                    .translate(0, 0, -3)
                    .rotateX(-pitch) 
                    .rotateY(-yaw)
                    .rotate(new Quaternionf(myboat.rotation).invert())
                    .translate(new Vector3f(myboat.position).negate());
            }









            glfwSwapBuffers(Window.id);
            

            
            long render_time = System.nanoTime() - render_start_time;
            long delta_ns = System.nanoTime() - start_time;
            double delta_ms = delta_ns / 1_000_000.0;
            String performanceInfo = String.format("frame time: %.1fms | render time: %.1fms", 
            delta_ms, render_time/1_000_000.0);
            glfwSetWindowTitle(Window.id, WINDOW_NAME + " | " + performanceInfo);
        }
        cleanup();
        Opengl.cleanup();
    }
    public static void cleanup() throws IOException{
        Mesh.cleanupAll();
        Window.cleanUp();
    }
}
