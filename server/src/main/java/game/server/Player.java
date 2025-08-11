package game.server;

import java.io.ObjectOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;

import game.common.Boat;
import game.common.DamageEvent;
import game.common.FireEvent;

public class Player implements Runnable {
    public Socket socket;
    public ObjectInputStream in;
    public ObjectOutputStream out;

    public Boat boat = new Boat();
    // this boat is written exclusively from the player that owns it. 
    // the main thread reads all the player's boats at once and copies them into gamestate
    public FireEvent lastFired = null;
    public DamageEvent lastHit = null;

    public Player(Socket socket) {
        this.socket = socket;
    }
    public int getid() {
        return Main.players_async.indexOf(this);
    }
    @Override
    public void run() {
        try {
            in = new ObjectInputStream(socket.getInputStream());
            out = new ObjectOutputStream(socket.getOutputStream());
            Main.players_async.add(this);
            while (true) {
                lastFired = (FireEvent) in.readObject();
                lastHit = (DamageEvent) in.readObject();
                boat = (Boat) in.readObject();
            }
        } catch (IOException e) {
            // Connection lost - socket will be detected as closed in main loop
        } catch (ClassNotFoundException e) {
            System.err.println("could not read object. failed deserialization");
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("Error closing client socket: " + e);
            }
        }
    } 
}
