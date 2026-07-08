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
    private static Map<String, List<String>> pinyinToWordMap = new HashMap<>();
    private static Map<String, Double> wordFrequencyMap = new HashMap<>();
    private static int maxSyllableLength = 6;
    private static final int MAX_MULTI_CHAR_SUGGESTIONS = 50;
    private static final int MAX_WORD_SUGGESTIONS = 30;

    static {
        int languageMode = ConfigManager.getLanguageMode();
        loadDictionaries(languageMode);
        loadFrequencies(languageMode);
        loadWordDictionary(languageMode);
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

        maxSyllableLength = 6;
        for (String syllable : pinyinToHanziMapNoTones.keySet()) {
            if (syllable.length() > maxSyllableLength) {
                maxSyllableLength = syllable.length();
            }
        }
    }

    private static void loadFrequencies(int languageMode) {
        if (languageMode == 1 || languageMode == 2) {
            loadFrequencyData("frequency.json", hanziFrequencyMap);
        } else if (languageMode == 3) {
            loadFrequencyData("cantonese-f.json", hanziFrequencyMap);
        }
    }

    private static void loadWordDictionary(int languageMode) {
        if (languageMode == 1) {
            loadPinyinDictionary("simplified-words.json", pinyinToWordMap);
            loadFrequencyData("word-frequency.json", wordFrequencyMap);
        }
    }

    public static void setLanguageMode(int languageMode) {
        pinyinToHanziMapWithTones.clear();
        pinyinToHanziMapNoTones.clear();
        hanziFrequencyMap.clear();
        pinyinToWordMap.clear();
        wordFrequencyMap.clear();

        loadDictionaries(languageMode);
        loadFrequencies(languageMode);
        loadWordDictionary(languageMode);
    }

    private static void loadPinyinDictionary(String fileName, Map<String, List<String>> dictionaryMap) {
        Gson gson = new Gson();
        try {
            Identifier resourceId = Identifier.of("chineseime", fileName);
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
            Identifier resourceId = Identifier.of("chineseime", fileName);
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

    private static void loadFrequencyData(String fileName, Map<String, Double> targetMap) {
        Gson gson = new Gson();
        try {
            Identifier resourceId = Identifier.of("chineseime", fileName);
            ResourceManager resourceManager = MinecraftClient.getInstance().getResourceManager();
            Resource resource = resourceManager.getResource(resourceId).orElseThrow(() -> new RuntimeException("Resource not found: " + resourceId));

            try (InputStream inputStream = resource.getInputStream();
                 JsonReader reader = new JsonReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

                reader.beginObject();
                while (reader.hasNext()) {
                    String key = reader.nextName();
                    Double frequency = gson.fromJson(reader, Double.class);

                    targetMap.put(key, frequency);
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

        if (suggestions.isEmpty() && !input.matches(".*\\d$") && input.length() > 1) {
            String normalizedInput = normalizeInput(input, languageMode);

            List<String> wordSuggestions = getWordSuggestions(normalizedInput);
            if (!wordSuggestions.isEmpty()) {
                return wordSuggestions;
            }

            List<String> multiCharSuggestions = getMultiCharSuggestions(normalizedInput);
            if (!multiCharSuggestions.isEmpty()) {
                return multiCharSuggestions;
            }
        }

        return suggestions;
    }

    private static List<String> getWordSuggestions(String input) {
        if (pinyinToWordMap.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> exactMatches = new ArrayList<>(pinyinToWordMap.getOrDefault(input, Collections.emptyList()));
        exactMatches.sort((word1, word2) ->
                Double.compare(wordFrequencyMap.getOrDefault(word2, 0.0), wordFrequencyMap.getOrDefault(word1, 0.0)));

        List<String> prefixMatches = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : pinyinToWordMap.entrySet()) {
            if (entry.getKey().length() > input.length() && entry.getKey().startsWith(input)) {
                prefixMatches.addAll(entry.getValue());
            }
        }
        prefixMatches.sort((word1, word2) ->
                Double.compare(wordFrequencyMap.getOrDefault(word2, 0.0), wordFrequencyMap.getOrDefault(word1, 0.0)));

        List<String> suggestions = new ArrayList<>(exactMatches);
        for (String word : prefixMatches) {
            if (suggestions.size() >= MAX_WORD_SUGGESTIONS) {
                break;
            }
            if (!suggestions.contains(word)) {
                suggestions.add(word);
            }
        }

        return suggestions;
    }

    private static List<String> getMultiCharSuggestions(String input) {
        List<String> syllables = segmentPinyin(input);
        if (syllables.size() <= 1) {
            return Collections.emptyList();
        }

        StringBuilder prefixBuilder = new StringBuilder();
        for (int i = 0; i < syllables.size() - 1; i++) {
            List<String> candidates = pinyinToHanziMapNoTones.get(syllables.get(i));
            if (candidates == null || candidates.isEmpty()) {
                return Collections.emptyList();
            }
            prefixBuilder.append(bestHanzi(candidates));
        }
        String prefix = prefixBuilder.toString();

        String lastSyllable = syllables.get(syllables.size() - 1);
        Set<String> lastCandidates = new HashSet<>();
        List<String> exactMatch = pinyinToHanziMapNoTones.get(lastSyllable);
        if (exactMatch != null) {
            lastCandidates.addAll(exactMatch);
        } else {
            for (Map.Entry<String, List<String>> entry : pinyinToHanziMapNoTones.entrySet()) {
                if (entry.getKey().startsWith(lastSyllable)) {
                    lastCandidates.addAll(entry.getValue());
                }
            }
        }

        if (lastCandidates.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> sortedLastCandidates = new ArrayList<>(lastCandidates);
        sortedLastCandidates.sort((hanzi1, hanzi2) ->
                Double.compare(hanziFrequencyMap.getOrDefault(hanzi2, 0.0), hanziFrequencyMap.getOrDefault(hanzi1, 0.0)));

        List<String> suggestions = new ArrayList<>();
        for (String hanzi : sortedLastCandidates) {
            suggestions.add(prefix + hanzi);
            if (suggestions.size() >= MAX_MULTI_CHAR_SUGGESTIONS) {
                break;
            }
        }

        return suggestions;
    }

    private static String bestHanzi(List<String> candidates) {
        String best = candidates.get(0);
        double bestFreq = hanziFrequencyMap.getOrDefault(best, 0.0);
        for (String candidate : candidates) {
            double freq = hanziFrequencyMap.getOrDefault(candidate, 0.0);
            if (freq > bestFreq) {
                best = candidate;
                bestFreq = freq;
            }
        }
        return best;
    }

    private static List<String> segmentPinyin(String input) {
        Map<Integer, List<String>> memo = new HashMap<>();
        List<String> full = segmentFull(input, 0, memo);
        if (full != null) {
            return full;
        }

        List<String> result = new ArrayList<>();
        int i = 0;
        int n = input.length();
        while (i < n) {
            int matchLen = -1;
            int maxLen = Math.min(maxSyllableLength, n - i);
            for (int len = maxLen; len >= 1; len--) {
                if (pinyinToHanziMapNoTones.containsKey(input.substring(i, i + len))) {
                    matchLen = len;
                    break;
                }
            }
            if (matchLen == -1) {
                result.add(input.substring(i));
                break;
            }
            result.add(input.substring(i, i + matchLen));
            i += matchLen;
        }
        return result;
    }

    private static List<String> segmentFull(String input, int start, Map<Integer, List<String>> memo) {
        if (start == input.length()) {
            return new ArrayList<>();
        }
        if (memo.containsKey(start)) {
            return memo.get(start);
        }

        int maxLen = Math.min(maxSyllableLength, input.length() - start);
        for (int len = maxLen; len >= 1; len--) {
            String candidate = input.substring(start, start + len);
            if (pinyinToHanziMapNoTones.containsKey(candidate)) {
                List<String> rest = segmentFull(input, start + len, memo);
                if (rest != null) {
                    List<String> result = new ArrayList<>();
                    result.add(candidate);
                    result.addAll(rest);
                    memo.put(start, result);
                    return result;
                }
            }
        }

        memo.put(start, null);
        return null;
    }
}