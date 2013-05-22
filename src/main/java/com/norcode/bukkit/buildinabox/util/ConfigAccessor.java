package com.norcode.bukkit.buildinabox.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import com.norcode.bukkit.buildinabox.BuildInABox;

public class ConfigAccessor {

    private final String fileName;
    private final JavaPlugin plugin;

    private File configFile;
    private FileConfiguration fileConfiguration;

    public ConfigAccessor(JavaPlugin plugin, String fileName) {
        if (plugin == null)
            throw new IllegalArgumentException("plugin cannot be null");
        if (!plugin.isInitialized())
            throw new IllegalArgumentException("plugin must be initiaized");
        this.plugin = plugin;
        this.fileName = fileName;
    }

    public void reloadConfig() {
        if (configFile == null) {
            File dataFolder = plugin.getDataFolder();
            if (dataFolder == null)
                throw new IllegalStateException();
            configFile = new File(dataFolder, fileName);
        }
        fileConfiguration = YamlConfiguration.loadConfiguration(configFile);

        // Look for defaults in the jar
        InputStream defConfigStream = plugin.getResource(fileName);
        if (defConfigStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
            fileConfiguration.setDefaults(defConfig);
        }
    }

    public FileConfiguration getConfig() {
        if (fileConfiguration == null) {
            this.reloadConfig();
        }
        return fileConfiguration;
    }

    /**
     * Load a file from the jar and save it to the filesystem without angering windows.
     * thanks https://github.com/alkarinv/BattleArena/
     */
    public static File saveResource(Class<?> clazz, String config_file, String default_file) {
        File file = new File(config_file);
        if (!file.exists()){ // Create a new file from our default example
            InputStream inputStream = null;
            OutputStream out = null;
            try{
                inputStream = clazz.getResourceAsStream(default_file);
                if (inputStream == null){ // will this work to fix the problems in windows??
                    inputStream = clazz.getClassLoader().getResourceAsStream(default_file);}

                out=new FileOutputStream(config_file);
                byte buf[]=new byte[1024];
                int len;
                while((len=inputStream.read(buf))>0){
                    out.write(buf,0,len);}
            } catch (Exception e){
                e.printStackTrace();
            } finally {
                if (out != null) try {out.close();} catch (Exception e){}
                if (inputStream != null) try {inputStream.close();} catch (Exception e){}
            }
        }
        return file;
    }

    public void saveConfig() {
        if (fileConfiguration == null || configFile == null) {
            return;
        } else {
            try {
                getConfig().save(configFile);
            } catch (IOException ex) {
                plugin.getLogger().log(Level.SEVERE, "Could not save config to " + configFile, ex);
            }
        }
    }

    public void saveDefaultConfig() {
        saveResource(BuildInABox.class, new File(BuildInABox.getInstance().getDataFolder(),fileName).getPath(), fileName);
//        if (!configFile.exists()) {
//            this.plugin.saveResource(fileName, false);
//        }
    }
}