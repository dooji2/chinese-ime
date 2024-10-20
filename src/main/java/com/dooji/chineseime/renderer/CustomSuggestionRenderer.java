package com.dooji.chineseime.renderer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class CustomSuggestionRenderer {
    private final MinecraftClient client;
    public List<String> suggestions;
    private int selectedIndex = 0;
    private int offset = 0;
    private final int maxVisibleSuggestions;
    private final int color;
    private final int scrollbarWidth = 4;
    private long lastArrowKeyTime = 0;
    private final long arrowKeyDelay = 150;

    public CustomSuggestionRenderer(MinecraftClient client, List<String> suggestions, int maxVisibleSuggestions, int color) {
        this.client = client;
        this.suggestions = suggestions;
        this.maxVisibleSuggestions = maxVisibleSuggestions;
        this.color = color;
    }

    public void updateSuggestions(List<String> newSuggestions) {
        this.suggestions = newSuggestions;
        this.selectedIndex = 0;
        this.offset = 0;
    }

    public void render(MatrixStack matrices, int x, int y, int width, int mouseX, int mouseY) {
        if (suggestions.isEmpty()) {
            return;
        }

        offset = MathHelper.clamp(offset, 0, Math.max(0, suggestions.size() - maxVisibleSuggestions));

        int visibleCount = Math.min(suggestions.size(), maxVisibleSuggestions);
        int totalHeight = visibleCount * 12;

        int startY = y;
        DrawableHelper.fill(matrices, x, startY - totalHeight, x + width - scrollbarWidth, startY, this.color);

        TextRenderer textRenderer = client.textRenderer;

        for (int i = 0; i < visibleCount; i++) {
            int suggestionIndex = i + offset;

            if (suggestionIndex >= 0 && suggestionIndex < suggestions.size()) {
                String suggestion = suggestions.get(suggestionIndex);

                int suggestionY = startY - (i + 1) * 12;

                int textColor = suggestionIndex == selectedIndex ? 0xFFFFFF : 0xAAAAAA;
                int backgroundColor = suggestionIndex == selectedIndex ? 0x555555 : this.color;

                DrawableHelper.fill(matrices, x, suggestionY, x + width - scrollbarWidth, suggestionY + 12, backgroundColor);
                textRenderer.drawWithShadow(matrices, suggestion, x + 2, suggestionY + 2, textColor);
            } else {
                System.out.println("Invalid suggestion index: " + suggestionIndex);
            }
        }

        if (suggestions.size() > maxVisibleSuggestions) {
            int scrollbarX = x + width - scrollbarWidth;
            int scrollbarHeight = Math.max((int) (((float) maxVisibleSuggestions / suggestions.size()) * totalHeight), 10);
            int scrollbarY = startY - (int) (((float) offset / (suggestions.size() - maxVisibleSuggestions)) * (totalHeight - scrollbarHeight));

            DrawableHelper.fill(matrices, scrollbarX, startY - totalHeight, scrollbarX + scrollbarWidth, startY, 0x80000000);
            DrawableHelper.fill(matrices, scrollbarX, scrollbarY - scrollbarHeight, scrollbarX + scrollbarWidth, scrollbarY, 0xFFFFFFFF);
        }
    }

    public void handleInput(int keyCode) {
        if (suggestions.isEmpty()) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastArrowKeyTime < arrowKeyDelay) {
            return;
        }
        lastArrowKeyTime = currentTime;

        switch (keyCode) {
            case GLFW.GLFW_KEY_UP:
                scroll(1);
                break;
            case GLFW.GLFW_KEY_DOWN:
                scroll(-1);
                break;
        }
    }

    private void scroll(int direction) {
        selectedIndex = MathHelper.clamp(selectedIndex + direction, 0, suggestions.size() - 1);

        if (selectedIndex < offset) {
            offset = selectedIndex;
        } else if (selectedIndex >= offset + maxVisibleSuggestions) {
            offset = selectedIndex - maxVisibleSuggestions + 1;
        }
    }

    public void handleScroll(double amount) {
        if (suggestions.isEmpty()) {
            return;
        }
        offset = MathHelper.clamp(offset + (int) amount, 0, suggestions.size() - maxVisibleSuggestions);
    }

    public boolean isMouseOverSuggestion(int mouseX, int mouseY, int startY, int width) {
        int visibleCount = Math.min(suggestions.size(), maxVisibleSuggestions);
        int suggestionHeight = 12;

        return mouseY >= startY - (visibleCount * suggestionHeight) && mouseY <= startY && mouseX < width - scrollbarWidth;
    }

    public String getSuggestionAt(int mouseX, int mouseY, int startY) {
        int suggestionHeight = 12;
        int visibleCount = Math.min(suggestions.size(), maxVisibleSuggestions);

        int relativeY = startY - mouseY;
        int index = relativeY / suggestionHeight;

        if (index >= 0 && index < visibleCount) {
            return suggestions.get(index + offset);
        } else {
            return null;
        }
    }

    public String getSelectedSuggestion() {
        if (suggestions.isEmpty()) {
            return null;
        }
        return suggestions.get(selectedIndex);
    }
}