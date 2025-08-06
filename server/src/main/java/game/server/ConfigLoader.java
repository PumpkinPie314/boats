package game.server;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

import game.common.Config;
import game.common.GameState;
public class ConfigLoader {
    public static void createDefault(String filepath){
        try {
            String default_config = ""; // TODO!
            Files.writeString(Paths.get(Main.CONFIG_FILE), default_config);
        } catch (IOException e) {
            System.err.println("could not write/create a new config file");
        }
    }
    public static Config load(String filepath) throws IOException {
        Config newConfig = new Config();
        ArrayList<String> availableFields = Arrays.stream(Config.class.getFields()).map(Field::getName).collect(Collectors.toCollection(ArrayList::new));
        ArrayList<String> assignedFields = new ArrayList<>(){};
        Files.lines(Paths.get(filepath)).forEach(l -> {
            String line = l;
            if (line.startsWith("#")) return; // line is a comment
            if (line.contains("#")) line = line.substring(0, line.indexOf('#')); // strip end of line comments
            String[] parts = line.split("=");
            String lhs = parts[0].trim();
            String rhs = parts[1].trim();
            try {
                Field field = Config.class.getField(lhs);
                if (field.getType() == float.class) {field.set(newConfig, parseFloat(rhs)); System.out.println("set " + lhs + " to " + rhs + " (" + parseFloat(rhs) +")");}
                else if (field.getType() == String.class) field.set(newConfig, rhs);
                else {
                    throw new IllegalArgumentException("config field type: " + field.getType().getName() + " is not supported yet!");
                }
                assignedFields.add(lhs);
            } catch (NoSuchFieldException | SecurityException e) {
                e.printStackTrace();
                throw new IllegalArgumentException("error reading config file: " + lhs + " is not a valid config field");
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        });
        availableFields.removeAll(assignedFields);
        if (!availableFields.isEmpty()) throw new IllegalStateException(
            "config file did not define the fallowing feilds: " +
            availableFields.stream().map(f->{
                return "\n" + f + " = ";
            }).collect(Collectors.joining())
        );
        return newConfig;
    }
    public static float parseFloat(String s) {
        if (s.contains("/")) {
            String[] fraction = s.split("/");
            Float numerator = parseFloat(fraction[0].trim());
            Float denominator = parseFloat(fraction[1].trim());
            return numerator/denominator;
        }
        if (s.contains("tau")) return parseFloat(s.replace("tau", "").trim()) * (float) Math.TAU;
        if (s.contains("pi")) return parseFloat(s.replace("pi", "").trim()) * (float) Math.PI;
        return Float.parseFloat(s);
    }
    public static void watchForConfigChanged(String filepath, GameState dest) {
        try {
            WatchService watcher = FileSystems.getDefault().newWatchService();
            Paths.get(".").register(watcher, StandardWatchEventKinds.ENTRY_MODIFY);
            while (true) {
                WatchKey key = watcher.take();
                for (WatchEvent<?> event : key.pollEvents()){
                    if (!event.context().toString().equals(filepath)) continue; // some other file in the root was modified
                    if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
                        System.out.println("config file change detected! reloading...");
                        dest.config = ConfigLoader.load(filepath);
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
