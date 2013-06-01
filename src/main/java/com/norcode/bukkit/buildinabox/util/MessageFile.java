package com.norcode.bukkit.buildinabox.util;

import com.norcode.bukkit.buildinabox.BuildInABox;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.logging.Level;

public class MessageFile extends ConfigAccessor {

    public MessageFile(JavaPlugin plugin, String fileName) {
        super(plugin, fileName);
    }

    private YamlConfiguration configFromUTF8Stream(InputStream stream) {
        StringBuffer buff = new StringBuffer();
        String line;
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(stream, "UTF8"));
            
            while ((line = in.readLine()) != null) {
                buff.append(line + "\r\n");
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (stream != null) try {stream.close();} catch (Exception e){}
        }
        YamlConfiguration cfg = new YamlConfiguration();
        try {
            cfg.loadFromString(buff.toString());
        } catch (InvalidConfigurationException e) {
            cfg = new YamlConfiguration();
        }
        return cfg;
    }

    public void reloadConfig() {
        if (configFile == null) {
            File dataFolder = plugin.getDataFolder();
            if (dataFolder == null)
                throw new IllegalStateException();
            configFile = new File(dataFolder, fileName);
        }
        try {
            fileConfiguration = configFromUTF8Stream(new FileInputStream(configFile));
        } catch (FileNotFoundException e) {
            fileConfiguration = new YamlConfiguration();
        }
        
        InputStream defIs = getPluginResource(fileName);
        if (defIs != null) {
            YamlConfiguration defCfg = configFromUTF8Stream(defIs);
            fileConfiguration.setDefaults(defCfg);
        }
    }

    public FileConfiguration getConfig() {
        if (fileConfiguration == null) {
            this.reloadConfig();
        }
        return fileConfiguration;
    }

    private static InputStream getPluginResource(String path) {
        InputStream inputStream = null;
        inputStream = BuildInABox.class.getResourceAsStream(path);
        if (inputStream == null){ // will this work to fix the problems in windows??
            inputStream = BuildInABox.class.getClassLoader().getResourceAsStream(path);
        }
        return inputStream;
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
                inputStream = getPluginResource(default_file);

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

    @Override
    public void saveDefaultConfig() {
        saveResource(BuildInABox.class, new File(BuildInABox.getInstance().getDataFolder()+"/"+fileName).getPath(), fileName);
    }
}