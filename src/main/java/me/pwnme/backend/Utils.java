package me.pwnme.backend;

import org.apache.commons.io.FileUtils;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.Map;

public class Utils {

    public static void createFile() throws FileNotFoundException {
        try {
            File file = new File("database.txt");
            if (file.createNewFile()){
                FileUtils.copyInputStreamToFile(BackendApplication.class.getResourceAsStream("database.yml"), file);
                System.out.println("File created: " + file.getName());

                InputStream inputStream = new FileInputStream(file);

                Yaml yaml = new Yaml();
                Map<String, Object> data = yaml.load(inputStream);
                System.out.println(data);
            }
            else
                System.out.println("File already exists.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
    public static void saveFile(FileConfiguration config, String path){

        File file = new File(Bukkit.getServer().getPluginManager().getPlugin("SkyBank").getDataFolder(), path);
        try {
            if(!file.exists()){
                file.createNewFile();
            }
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();

        }
    }

    public static FileConfiguration loadFile(String path, LoadFileType type) throws FileNotFoundException {

        File file = new File(Bukkit.getServer().getPluginManager().getPlugin("SkyBank").getDataFolder(), path);
        if (file.exists()) {
            return YamlConfiguration.loadConfiguration(file);
        } else {
            if(type == LoadFileType.PLAYER_DATA_READ_ONLY)
                throw new FileNotFoundException();
            else
                createFile(path, type);
            return loadFile(path, type);
        }
    }

     */
    
}
