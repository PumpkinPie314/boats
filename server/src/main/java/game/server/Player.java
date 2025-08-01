package game.server;

import java.io.ObjectOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;

import game.common.Boat;
import game.common.InputState;

public class Player implements Runnable {
    public Socket socket;
    public ObjectInputStream in;
    public ObjectOutputStream out;

    public InputState inputState;
    public Boat boat;

    public Player(Socket socket) {
        this.inputState = new InputState();
        this.socket = socket;
        this.boat = new Boat();
    }
    public int getid() {
        return Main.players.indexOf(this);
    }
    @Override
    public void run() {
        try {
            in = new ObjectInputStream(socket.getInputStream());
            out = new ObjectOutputStream(socket.getOutputStream());
            while (true) {
                inputState = (InputState) in.readObject();
            }
        } catch (IOException e) {
            System.out.println("player "+this.getid()+" has disconnected");
        } catch (ClassNotFoundException e) {
            System.err.println("could not read object into inputstate. failed deserialization");
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
