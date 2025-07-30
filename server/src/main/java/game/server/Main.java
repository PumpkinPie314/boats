package game.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
    public static final int PORT = 52025;
    public static void main(String[] args) throws IOException{

        System.out.println("Hello from server! listening on port: " + PORT);
        ServerSocket serverSocket = new ServerSocket(PORT);
        while (true) {
            Socket socket = serverSocket.accept();
            System.out.println("a new client has connected");
            
            new Thread(()-> {try{
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream());

                in.lines()
                    .forEach(l -> out.println(l));

            }catch (IOException e) {/*unreachable*/}}).start();
        }
    }
}
