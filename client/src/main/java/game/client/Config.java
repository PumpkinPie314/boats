package game.client;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.lwjgl.glfw.GLFW;


public class Config {
    public static String serverip;
    public static int serverport;
    public static int sailup;
    public static int saildown;
    public static int wheelleft;
    public static int wheelright;
    public static int cannonleft;
    public static int cannonright;

    public static void createDefault(){
        try {
            String default_config = """
            serverip = localhost
            serverport = 52025
            saildown = GLFW_KEY_W
            sailup = GLFW_KEY_S
            wheelleft = GLFW_KEY_A
            wheelright = GLFW_KEY_D
            cannonleft = GLFW_KEY_X
            cannonright = GLFW_KEY_C
            """;
            Files.writeString(Paths.get(Main.CONFIG_FILE), default_config);
        } catch (IOException e) {
            System.err.println("could not write/create a new config file");
        }
    }

    public static void load(String filepath){
        try {
            Files.lines(Paths.get(filepath))
                .filter(s -> s.matches("\\s*\\w+\\s*=\\s*\\w+\\s*"))
                .map(line -> line.split("\\s*=\\s*", 2))
                .forEach(pair -> {
                    String fieldName = pair[0].trim();
                    String value = pair[1].trim();
                    try {
                        Field field = Config.class.getField(fieldName);
                        Class<?> type = field.getType();
                        System.out.print("setting "+ fieldName+": ");
                        if (type == String.class) {
                            field.set(null, value);
                            System.out.println(value);
                        } else if (type == int.class) {
                            Field glfwField = GLFW.class.getField(value);
                            int intValue = glfwField.getInt(null);
                            System.out.println(intValue);
                            field.setInt(null, intValue);
                        }
                    } catch (IllegalAccessException | SecurityException | NoSuchFieldException e) {
                        System.err.println("failed to parse config file: " + e);
                    }
            });  
        } catch (IOException e) {
            System.err.println("failed to parse config file: " + e);
        }
    }
    // I hate this code, I don't like how indented it is :'( 
    // try catch is ugly

}
