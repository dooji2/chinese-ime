package com.dooji.chineseime.processing;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class PinyinDictionary {
    private static Map<String, List<String>> pinyinToHanziMapWithTones = new HashMap<>();
    private static Map<String, List<String>> pinyinToHanziMapNoTones = new HashMap<>();
    private static Map<String, Double> hanziFrequencyMap = new HashMap<>();

    static {
        int languageMode = ConfigManager.getLanguageMode();
        loadDictionaries(languageMode);
        loadFrequencies(languageMode);
    }

    private static void loadDictionaries(int languageMode) {
        if (languageMode == 1) {
            loadToneBasedPinyinDictionary("simplified-t.json", pinyinToHanziMapWithTones);
            loadPinyinDictionary("simplified.json", pinyinToHanziMapNoTones);
        } else if (languageMode == 2) {
            loadToneBasedPinyinDictionary("traditional-t.json", pinyinToHanziMapWithTones);
            loadPinyinDictionary("traditional.json", pinyinToHanziMapNoTones);
        } else if (languageMode == 3) {
            loadPinyinDictionary("cantonese.json", pinyinToHanziMapNoTones);
            loadPinyinDictionary("cantonese-t.json", pinyinToHanziMapWithTones);
        }
    }

    private static void loadFrequencies(int languageMode) {
        if (languageMode == 1 || languageMode == 2) {
            loadFrequencyData("frequency.json");
        } else if (languageMode == 3) {
            loadFrequencyData("cantonese-f.json");
        }
    }

    public static void setLanguageMode(int languageMode) {
        pinyinToHanziMapWithTones.clear();
        pinyinToHanziMapNoTones.clear();
        hanziFrequencyMap.clear();

        loadDictionaries(languageMode);
        loadFrequencies(languageMode);
    }

    private static void loadPinyinDictionary(String fileName, Map<String, List<String>> dictionaryMap) {
        Gson gson = new Gson();
        try {
            Identifier resourceId = new Identifier("chineseime", fileName);
            ResourceManager resourceManager = MinecraftClient.getInstance().getResourceManager();
            Resource resource = resourceManager.getResource(resourceId).orElseThrow(() -> new RuntimeException("Resource not found: " + resourceId));

            try (InputStream inputStream = resource.getInputStream();
                 JsonReader reader = new JsonReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

                int languageMode = ConfigManager.getLanguageMode();

                reader.beginObject();
                while (reader.hasNext()) {
                    String hanzi = reader.nextName();
                    List<String> pinyinList = gson.fromJson(reader, new TypeToken<List<String>>() {}.getType());

                    for (String pinyin : pinyinList) {
                        String normalizedPinyin = normalizeInput(pinyin, languageMode);
                        dictionaryMap.computeIfAbsent(normalizedPinyin, k -> new ArrayList<>()).add(hanzi);
                    }
                }
                reader.endObject();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void loadToneBasedPinyinDictionary(String fileName, Map<String, List<String>> dictionaryMap) {
        Gson gson = new Gson();
        try {
            Identifier resourceId = new Identifier("chineseime", fileName);
            ResourceManager resourceManager = MinecraftClient.getInstance().getResourceManager();
            Resource resource = resourceManager.getResource(resourceId).orElseThrow(() -> new RuntimeException("Resource not found: " + resourceId));

            try (InputStream inputStream = resource.getInputStream();
                 JsonReader reader = new JsonReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

                reader.beginObject();
                while (reader.hasNext()) {
                    String pinyinWithTone = reader.nextName();
                    List<String> hanziList = gson.fromJson(reader, new TypeToken<List<String>>() {}.getType());

                    dictionaryMap.put(pinyinWithTone, hanziList);
                }
                reader.endObject();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void loadFrequencyData(String fileName) {
        Gson gson = new Gson();
        try {
            Identifier resourceId = new Identifier("chineseime", fileName);
            ResourceManager resourceManager = MinecraftClient.getInstance().getResourceManager();
            Resource resource = resourceManager.getResource(resourceId).orElseThrow(() -> new RuntimeException("Resource not found: " + resourceId));

            try (InputStream inputStream = resource.getInputStream();
                 JsonReader reader = new JsonReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

                reader.beginObject();
                while (reader.hasNext()) {
                    String hanzi = reader.nextName();
                    Double frequency = gson.fromJson(reader, Double.class);

                    hanziFrequencyMap.put(hanzi, frequency);
                }
                reader.endObject();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String normalizeInput(String input, int languageMode) {
        if (languageMode == 3 || input.matches(".*\\d$")) {
            return input;
        } else {
            return input.replaceAll("[1-5]", "");
        }
    }

    public static List<String> getChineseSuggestions(String input) {
        int languageMode = ConfigManager.getLanguageMode();
        Set<String> uniqueSuggestions = new HashSet<>();

        if (input.matches(".*\\d$")) {
            uniqueSuggestions.addAll(pinyinToHanziMapWithTones.getOrDefault(input, Collections.emptyList()));
        } else {
            String normalizedInput = normalizeInput(input, languageMode);
            uniqueSuggestions.addAll(pinyinToHanziMapNoTones.getOrDefault(normalizedInput, Collections.emptyList()));

            if (languageMode == 3) {
                uniqueSuggestions.addAll(pinyinToHanziMapWithTones.getOrDefault(input, Collections.emptyList()));
            }
        }

        List<String> suggestions = new ArrayList<>(uniqueSuggestions);
        suggestions.sort((hanzi1, hanzi2) ->
                Double.compare(hanziFrequencyMap.getOrDefault(hanzi2, 0.0), hanziFrequencyMap.getOrDefault(hanzi1, 0.0)));

        return suggestions;
    }
}