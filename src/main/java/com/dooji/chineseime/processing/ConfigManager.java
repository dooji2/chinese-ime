package com.dooji.chineseime.processing;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ConfigManager {

    private static final String CONFIG_DIR = "config/ChineseIME";
    private static final String CONFIG_FILE_NAME = "config.json";
    private static final String CONFIG_KEY_LANGUAGE_MODE = "languageMode";
    private static final Gson gson = new Gson();
    private static File configFile;
    private static boolean isSimplified = true;

    public static void init() {
        File configDir = new File(MinecraftClient.getInstance().runDirectory, CONFIG_DIR);
        if (!configDir.exists()) {
            configDir.mkdirs();
        }

        configFile = new File(configDir, CONFIG_FILE_NAME);

        if (configFile.exists()) {
            loadConfig();
        } else {
            saveConfig();
        }
    }

    public static boolean isSimplifiedMode() {
        return isSimplified;
    }

    public static void setLanguageMode(boolean simplifiedMode) {
        isSimplified = simplifiedMode;
        saveConfig();
    }

    private static void loadConfig() {
        try (FileReader reader = new FileReader(configFile)) {
            JsonObject json = gson.fromJson(reader, JsonObject.class);
            if (json.has(CONFIG_KEY_LANGUAGE_MODE)) {
                isSimplified = json.get(CONFIG_KEY_LANGUAGE_MODE).getAsBoolean();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void saveConfig() {
        try (FileWriter writer = new FileWriter(configFile)) {
            JsonObject json = new JsonObject();
            json.addProperty(CONFIG_KEY_LANGUAGE_MODE, isSimplified);
            gson.toJson(json, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}