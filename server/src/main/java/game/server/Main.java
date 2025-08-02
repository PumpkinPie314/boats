package game.server;


import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.FileSystems;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import game.common.Boat;
import game.common.GameState;
import game.common.InputState;

public class Main {
    public static final int PORT = 52025;
    public static final String CONFIG_FILE = "server-config.txt";
    public static final List<Player> players = Collections.synchronizedList(new ArrayList<>());
    public static final GameState gameState = new GameState();
    public static final int target_fps = 20;
    public static boolean serverRunning = true;
    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.println("Hello from server!");
        System.out.println("reading config...");
        File config_file = new File(CONFIG_FILE);
        if (!config_file.exists()) {
            Config.createDefault();
            System.out.println("default config created");
        }
        Config.load(CONFIG_FILE);
        new Thread(()->watchForConfigChanged()).start();;

        System.out.println("listening on port: " + PORT);
        
        new Thread(()->listenForClients()).start();
        while (serverRunning) {
            long start_time = System.nanoTime(); 

            // give new players a boat
            if (players.size() > gameState.boats.size()) {
                System.out.println("A new client has connected! Welcome player " + (players.size() - 1));
                gameState.boats.add(players.getLast().boat);
                Thread.sleep(1000);//wait for player socket to connect
            }
            // despawn boats from disconnected players
            for (int i = players.size() - 1; i >= 0; i--) {
                if (players.get(i).socket.isClosed()) {
                    players.remove(i);
                    gameState.boats.remove(i);
                }
            };
            for (int i = 0; i < players.size(); i++) {
                assert players.get(0).boat == gameState.boats.get(0);
            }

            float dt = 1f/target_fps;
            // apply player inputs
            for (Player player : players) {
                Boat boat = player.boat;
                InputState inputs = player.inputState;
                if (inputs.saildown) boat.mast_down_percent += Config.mast_drop_speed * dt;
                if (inputs.sailup) boat.mast_down_percent -= Config.mast_raise_speed * dt;
                if (inputs.wheelleft) boat.wheel_turn_percent -= Config.wheel_turn_speed * dt;
                if (inputs.wheelright) boat.wheel_turn_percent += Config.wheel_turn_speed * dt;
                if (inputs.sailleft) boat.sail_turn_percent -= Config.sail_turn_speed * dt;
                if (inputs.sailright) boat.sail_turn_percent += Config.sail_turn_speed * dt;
                boat.cannonRotation = inputs.cannon_rotation;

                if (boat.mast_down_percent > 1f) boat.mast_down_percent = 1f;
                if (boat.mast_down_percent < 0f) boat.mast_down_percent = 0f;
                if (boat.wheel_turn_percent > 1f) boat.wheel_turn_percent = 1f;
                if (boat.wheel_turn_percent < -1f) boat.wheel_turn_percent = -1f;
                if (boat.sail_turn_percent > 1f) boat.sail_turn_percent = 1f;
                if (boat.sail_turn_percent < -1f) boat.sail_turn_percent = -1f;
            }

            // game logic
            for (Boat boat : gameState.boats) {
                Vector3f forward = new Vector3f(0,0,1).rotate(boat.rotation);
                // gameState.wind_direction = boat.rotation; // temp!
                boat.velocity = forward.mul(boat.mast_down_percent * Config.sail_speed);
                boat.rotation.integrate(dt, 0, boat.wheel_turn_percent * Config.turn_speed * -1,0);

                boat.position.add(boat.velocity.mul(dt));
            }




            // send gamestate to players
            for (int i = 0; i < players.size(); i++) {
                Player player = players.get(i);
                player.out.reset();
                player.out.writeInt(i);//so the client know which boat is myboat
                player.out.writeObject(gameState);
                player.out.flush();
            }
            long delta_ns = System.nanoTime() - start_time;
            double delta_ms = delta_ns / 1_000_000.0;
            double fps = 1000.0 / delta_ms;
            if (fps > target_fps) {
                Thread.sleep(1000L/target_fps - (long)delta_ms);
                fps = target_fps;
            }            
        }
    }
    public static void listenForClients() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket socket = serverSocket.accept();
                socket.setTcpNoDelay(true);
                Player player = new Player(socket);
                players.add(player);
                
                new Thread(player).start();
            }
        } catch (IOException e) {
            System.err.println("Server socket error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    public static void watchForConfigChanged() {
        try {
            WatchService watcher = FileSystems.getDefault().newWatchService();
            Paths.get(".").register(watcher, StandardWatchEventKinds.ENTRY_MODIFY);
            while (true) {
                WatchKey key = watcher.take();
                for (WatchEvent<?> event : key.pollEvents()){
                    if (!event.context().toString().equals(CONFIG_FILE)) continue; // some other file in the root was modified
                    if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
                        System.out.println("config file change detected! reloading...");
                        Config.load(CONFIG_FILE);
                        System.out.println("config file reloaded!");
                    }
                }
                key.reset();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
