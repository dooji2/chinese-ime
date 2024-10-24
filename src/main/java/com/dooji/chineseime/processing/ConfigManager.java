package com.dooji.chineseime.processing;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
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
    private static int languageMode = 1;

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

    public static int getLanguageMode() {
        return languageMode;
    }

    public static void setLanguageMode(int mode) {
        languageMode = mode;
        saveConfig();
    }

    private static void loadConfig() {
        try (FileReader reader = new FileReader(configFile)) {
            JsonObject json = gson.fromJson(reader, JsonObject.class);

            if (json.has(CONFIG_KEY_LANGUAGE_MODE)) {
                JsonElement languageModeElement = json.get(CONFIG_KEY_LANGUAGE_MODE);

                if (languageModeElement.isJsonPrimitive()) {
                    JsonPrimitive primitive = languageModeElement.getAsJsonPrimitive();

                    if (primitive.isBoolean()) {
                        boolean oldLanguageMode = primitive.getAsBoolean();
                        languageMode = oldLanguageMode ? 1 : 2;
                        saveConfig();
                    } else if (primitive.isNumber()) {
                        languageMode = primitive.getAsInt();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void saveConfig() {
        try (FileWriter writer = new FileWriter(configFile)) {
            JsonObject json = new JsonObject();
            json.addProperty(CONFIG_KEY_LANGUAGE_MODE, languageMode);
            gson.toJson(json, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}