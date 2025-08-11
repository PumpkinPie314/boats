package game.server;


import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import game.common.Config;
import game.common.GameState;

public class Main {
    public static final int PORT = 52025;
    public static final String CONFIG_FILE = "config.txt";
    public static final List<Player> players_async = new ArrayList<>();
                                   //players_async is a bunch of threads running in parrelel. one for each client
    public static final GameState gameState = new GameState();
    public static boolean serverRunning = true;
    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.println("Hello from server!");
        System.out.println("reading config...");
        File config_file = new File(CONFIG_FILE);
        if (!config_file.exists()) {
            ConfigLoader.createDefault(CONFIG_FILE);
            System.out.println("default config created");
        }
        gameState.config = ConfigLoader.load(CONFIG_FILE);
        new Thread(()->ConfigLoader.watchForConfigChanged(CONFIG_FILE, gameState)).start();;

        System.out.println("listening on port: " + PORT);
        
        // a little thread who just waits for client connections and puts them in the players list
        new Thread(()->listenForClients()).start();

        // this players array is updated every frame synchronously
        List<Player> players = new ArrayList<>();

        gameState.tick_current = 0;
        while (serverRunning) {
            long start_time = System.nanoTime(); 
            gameState.tick_current += 1;
            synchronized (players_async) {
                players = new ArrayList<>(players_async);
            }
            // despawn boats from disconnected players
            for (int i = players.size() - 1; i >= 0; i--) {
                if (players.get(i).socket.isClosed()) {
                    System.out.println("player " + i + " has disconnected");
                    if (i+1 > players.size()) System.out.println("player numbers shifted");
                    players.remove(i);
                    gameState.boats.remove(i);
                    gameState.fireEvents.remove(i);
                    gameState.damageEvents.remove(i);
                    synchronized (players_async) {
                        players_async.remove(i);
                    }
                }
            };
            // give new players a boat
            if (players.size() > gameState.boats.size()) {
                int new_players = players.size() - gameState.boats.size();
                for (int i = 0; i < new_players; i++) {
                    int playerId = gameState.boats.size(); // This will be the correct next available ID
                    System.out.println("A new client has connected! Welcome player " + playerId);
                    gameState.boats.add(players.get(playerId).boat);
                    gameState.fireEvents.add(null);
                    gameState.damageEvents.add(null);
                }
                Thread.sleep(1000);//wait for player socket to connect
            }
            for (int i = 0; i < players.size(); i++) {
                assert players.get(0).boat == gameState.boats.get(0);
            }
            // read incomming boat data from clients
            for (int i = 0; i < players.size(); i++) {
                Player p = players.get(i);
                gameState.boats.set(i, p.boat);
                gameState.fireEvents.set(i, p.lastFired);
                gameState.damageEvents.set(i, p.lastHit);
                // System.out.println("player " + i + "'s position has ben set to: "+ gameState.boats.get(i).position);
            }

            // update config 
            Config config = gameState.config;
        
            // game logic
            gameState.wind = new Vector3f(0,0,1);
            /*
             * most of the game logic is done by the clients. this is bad because clients can send whatever they want and cheat.
             * I just don't want to have to write every line of code twice: once for client prediction and again for server validation.
             * perhaps in the future, if cheaters are an issue, I can copy some client code over here to validate packets.
             */
            


            // send gamestate to players
            for (int i = 0; i < players.size(); i++) {
                Player player = players.get(i);
                try {
                    player.out.reset();
                    player.out.writeInt(i);//so the client know which boat is myboat
                    player.out.writeObject(gameState);
                    player.out.flush();
                } catch (SocketException e) {
                    player.socket.close();;
                }
            }
            long delta_ns = System.nanoTime() - start_time;
            double delta_ms = delta_ns / 1_000_000.0;
            double fps = 1000.0 / delta_ms;
            if (fps > config.fps) {
                Thread.sleep(1000L/(long)config.fps - (long)delta_ms);
            }
            // System.out.println(1000L/(long)config.fps - (long)delta_ms);
            // System.out.println(1000L/(long)gameState.config.fps - (long)delta_ms);
        }
    }
    public static void listenForClients() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket socket = serverSocket.accept();
                socket.setTcpNoDelay(true);
                Player player = new Player(socket);
                new Thread(player).start();
            }
        } catch (IOException e) {
            System.err.println("Server socket error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
