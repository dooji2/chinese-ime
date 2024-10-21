package com.dooji.chineseime;

import com.dooji.chineseime.processing.ConfigManager;
import com.dooji.chineseime.processing.PinyinDictionary;
import com.dooji.chineseime.mixin.ChatScreenAccessor;
import com.dooji.chineseime.renderer.CustomSuggestionRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class IMEHandler {
    private static MinecraftClient client;
    private static CustomSuggestionRenderer suggestionRenderer;
    private String lastInput = "";

    public IMEHandler(MinecraftClient client) {
        IMEHandler.client = client;
    }

    public void showSuggestions(List<String> suggestions) {
        if (suggestionRenderer == null) {
            suggestionRenderer = new CustomSuggestionRenderer(client, suggestions, 10, 0x80000000);
        } else {
            suggestionRenderer.updateSuggestions(suggestions);
        }
    }

    public boolean isSuggestionListActive() {
        return suggestionRenderer != null && !suggestionRenderer.suggestions.isEmpty();
    }

    public void renderCustomSuggestions(MatrixStack matrices) {
        if (suggestionRenderer != null && !suggestionRenderer.suggestions.isEmpty()) {
            TextFieldWidget chatField = ((ChatScreenAccessor) client.currentScreen).getChatField();
            if (chatField != null) {
                int chatBoxY = chatField.y - 6;
                suggestionRenderer.render(matrices, 4, chatBoxY, 150, 0, 0);
            }
        }
    }

    public void handleInput(int keyCode) {
        if (suggestionRenderer != null && !suggestionRenderer.suggestions.isEmpty()) {
            suggestionRenderer.handleInput(keyCode);

            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_TAB) {
                String selectedSuggestion = suggestionRenderer.getSelectedSuggestion();
                if (selectedSuggestion != null) {
                    insertSuggestionIntoChatField(selectedSuggestion);
                }
            }
        }
    }

    private void insertSuggestionIntoChatField(String suggestion) {
        if (client.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen) {
            TextFieldWidget chatField = ((ChatScreenAccessor) client.currentScreen).getChatField();
            if (chatField != null) {
                String currentText = chatField.getText();
                String lastPinyin = extractLastPinyin(currentText);

                if (!lastPinyin.isEmpty()) {
                    String pinyinPattern = "(?<=\\b|\\d)([a-zA-Z]+\\d?)$";

                    if (currentText.matches(".*" + pinyinPattern)) {
                        String newText = currentText.replaceFirst(pinyinPattern, suggestion);

                        chatField.setText(newText);
                    } else {
                        chatField.setText(currentText + suggestion);
                    }

                    showSuggestions(List.of());
                    updateSuggestionsBasedOnInput();
                } else {
                    chatField.setText(currentText + suggestion);
                }
            }
        }
    }

    public void handleMouseClick(int mouseX, int mouseY) {
        if (suggestionRenderer != null) {
            TextFieldWidget chatField = ((ChatScreenAccessor) client.currentScreen).getChatField();
            if (chatField != null) {
                int chatBoxY = chatField.y - 12;
                int suggestionWidth = 150;
                if (suggestionRenderer.isMouseOverSuggestion(mouseX, mouseY, chatBoxY, suggestionWidth)) {
                    String selectedSuggestion = suggestionRenderer.getSuggestionAt(mouseX, mouseY, chatBoxY);
                    if (selectedSuggestion != null) {
                        insertSuggestionIntoChatField(selectedSuggestion);
                    }
                }
            }
        }
    }

    public void handleMouseScroll(double amount) {
        if (suggestionRenderer != null) {
            suggestionRenderer.handleScroll(amount);
        }
    }

    public void updateSuggestionsBasedOnInput() {
        if (client.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen) {
            TextFieldWidget chatField = ((ChatScreenAccessor) client.currentScreen).getChatField();
            if (chatField != null) {
                String currentText = chatField.getText();

                if (currentText.startsWith("/") || currentText.endsWith(" ") || currentText.matches(".*[\u4e00-\u9fa5]+$")) {
                    showSuggestions(List.of());
                    lastInput = currentText;
                    return;
                }

                if (currentText.matches(".*\\s\\d$")) {
                    showSuggestions(List.of());
                    lastInput = currentText;
                    return;
                }

                String lastPinyin = extractLastPinyin(currentText);

                if (!currentText.equals(lastInput)) {
                    List<String> suggestions = PinyinDictionary.getChineseSuggestions(lastPinyin);
                    if (!suggestions.isEmpty()) {
                        showSuggestions(suggestions);
                    } else {
                        showSuggestions(List.of());
                    }
                    lastInput = currentText;
                }
            }
        }
    }

    public void toggleLanguageMode() {
        int currentMode = ConfigManager.getLanguageMode();
        int newMode;

        if (currentMode == 1) {
            newMode = 2;
        } else if (currentMode == 2) {
            newMode = 3;
        } else {
            newMode = 1;
        }

        ConfigManager.setLanguageMode(newMode);
        PinyinDictionary.setLanguageMode(newMode);
    }

    private String extractLastPinyin(String input) {
        String[] parts = input.split("[^a-zA-Z0-9]+");

        for (int i = parts.length - 1; i >= 0; i--) {
            String part = parts[i];

            part = part.replaceFirst("^\\d+", "");

            if (part.matches("[a-zA-Z]+\\d?")) {
                return part;
            }
        }

        return "";
    }
}