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
        boolean isSimplified = ConfigManager.isSimplifiedMode();
        loadDictionaries(isSimplified);
        loadFrequencyData("frequency.json");
    }

    private static void loadDictionaries(boolean isSimplified) {
        if (isSimplified) {
            loadPinyinDictionary("simplified.json", pinyinToHanziMapWithTones);
            loadPinyinDictionary("simplified-o.json", pinyinToHanziMapNoTones);
        } else {
            loadPinyinDictionary("traditional.json", pinyinToHanziMapWithTones);
            loadPinyinDictionary("traditional-o.json", pinyinToHanziMapNoTones);
        }
    }

    public static void setLanguageMode(boolean isSimplified) {
        pinyinToHanziMapWithTones.clear();
        pinyinToHanziMapNoTones.clear();
        loadDictionaries(isSimplified);
    }

    private static void loadPinyinDictionary(String fileName, Map<String, List<String>> dictionaryMap) {
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
                    List<String> pinyinList = gson.fromJson(reader, new TypeToken<List<String>>() {}.getType());

                    for (String pinyin : pinyinList) {
                        String normalizedPinyin = normalizePinyin(pinyin);
                        dictionaryMap.computeIfAbsent(normalizedPinyin, k -> new ArrayList<>()).add(hanzi);
                    }
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

    private static String normalizePinyin(String pinyin) {
        return pinyin.replaceAll("[1-5]", "");
    }

    public static List<String> getChineseSuggestions(String input) {
        Set<String> uniqueSuggestions = new HashSet<>();

        if (input.matches(".*\\d.*")) {
            uniqueSuggestions.addAll(pinyinToHanziMapWithTones.getOrDefault(input, Collections.emptyList()));
        }

        String normalizedInput = normalizePinyin(input);
        uniqueSuggestions.addAll(pinyinToHanziMapNoTones.getOrDefault(normalizedInput, Collections.emptyList()));

        List<String> suggestions = new ArrayList<>(uniqueSuggestions);

        suggestions.sort((hanzi1, hanzi2) ->
                Double.compare(hanziFrequencyMap.getOrDefault(hanzi2, 0.0), hanziFrequencyMap.getOrDefault(hanzi1, 0.0)));

        return suggestions;
    }
}