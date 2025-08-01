package game.server;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import game.common.Boat;
import game.common.GameState;
import game.common.InputState;

public class Main {
    public static final int PORT = 52025;
    public static final String CONFIG_FILE = "server-config.txt";
    public static final List<Player> players = Collections.synchronizedList(new ArrayList<>());
    public static final GameState gameState = new GameState();
    public static boolean serverRunning = true;
    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.println("Hello from server!");
        System.out.println("reading config...");
        File config_file = new File(CONFIG_FILE);
        if (!config_file.exists()) {
            Config.createDefault();
            System.out.println("default config created");
        }
        Config.load("server-config.txt");

        System.out.println("listening on port: " + PORT);
        
        new Thread(()->listenForClients()).start();

        final int target_fps = 20;
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

            // apply player inputs
            for (Player player : players) {
                Boat boat = player.boat;
                InputState inputs = player.inputState;
                boat.velocity.x += inputs.saildown ? 1 : 0;
                if (inputs.saildown) System.out.println("player " + player.getid() + " is pressing w");
            }

            // game logic
            for (Boat boat : gameState.boats) {
                boat.position.add(boat.velocity); 
            }
            if (!gameState.boats.isEmpty()) {
                gameState.boats.get(0).position.x += 1;
                // System.out.println("Server: boat 0 x position = " + gameState.boats.get(0).position.x);
                // System.out.println("boat ref:   " + gameState.boats.get(0).position.x);
                // System.out.println("player ref: " + players.get(0).boat.position.x);
            }
            
            // serialized the gamestate to one packet
            // ByteArrayOutputStream baos = new ByteArrayOutputStream();
            // ObjectOutputStream oos = new ObjectOutputStream(baos);
            // oos.writeObject(gameState);
            // oos.flush();
            // byte[] gamestate_packet = baos.toByteArray();

            // send gamestate to players
            for (int i = 0; i < players.size(); i++) {
                Player player = players.get(i);
                player.out.reset();
                player.out.writeInt(i);//so the client know which boat is myboat
                // player.out.writeInt(gamestate_packet.length);
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
    private static void listenForClients() {
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
}
