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
                    currentText = currentText.substring(0, currentText.length() - lastPinyin.length()) + suggestion;
                } else {
                    currentText += suggestion;
                }

                chatField.setText(currentText);

                updateSuggestionsBasedOnInput();
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

                if (currentText.startsWith("/")) {
                    showSuggestions(List.of());
                    return;
                }

                if (currentText.endsWith(" ")) {
                    showSuggestions(List.of());
                    return;
                }

                String lastPinyin = extractLastPinyin(currentText);

                if (!currentText.equals(lastInput)) {
                    lastInput = currentText;

                    if (!lastPinyin.isEmpty()) {
                        List<String> suggestions = PinyinDictionary.getChineseSuggestions(lastPinyin);
                        showSuggestions(suggestions);
                    } else {
                        showSuggestions(List.of());
                    }
                }
            }
        }
    }

    public void toggleLanguageMode(boolean isSimplifiedMode) {
        PinyinDictionary.setLanguageMode(isSimplifiedMode);
        ConfigManager.setLanguageMode(isSimplifiedMode);
    }

    private String extractLastPinyin(String input) {
        String[] parts = input.split("[\u4e00-\u9fa5\\s]+");
        return (parts.length > 0) ? parts[parts.length - 1] : "";
    }
}