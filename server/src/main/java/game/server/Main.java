package game.server;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
    public static final int PORT = 52025;
    public static final String CONFIG_FILE = "server-config.txt";
    public static void main(String[] args) throws IOException{
        System.out.println("Hello from server!");
        System.out.println("reading config...");
        File config_file = new File(CONFIG_FILE);
        if (!config_file.exists()) {
            Config.createDefault();
            System.out.println("default config created");
        }
        Config.load("server-config.txt");

        System.out.println("listening on port: " + PORT);
        ServerSocket serverSocket = new ServerSocket(PORT);
        while (true) {
            Socket socket = serverSocket.accept();
            System.out.println("a new client has connected");
            
            new Thread(()-> {
                try(
                    DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                    DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                ){
                    while (true) {
                        System.out.print(in.readUTF());
                    }
                }
                catch (IOException e) {/*unreachable*/}
            }).start();
        }
    }
}
