package com.dooji.chineseime.mixin;

import com.dooji.chineseime.processing.ConfigManager;
import com.dooji.chineseime.IMEHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {

    private final IMEHandler imeHandler = new IMEHandler(MinecraftClient.getInstance());

    @Inject(method = "render", at = @At("HEAD"))
    private void onRender(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen instanceof ChatScreen) {
            imeHandler.renderCustomSuggestions(matrices);

            int width = client.getWindow().getScaledWidth();

            String buttonText = "";
            int languageMode = ConfigManager.getLanguageMode();
            if (languageMode == 1) {
                buttonText = I18n.translate("chineseime.language.simplified");
            } else if (languageMode == 2) {
                buttonText = I18n.translate("chineseime.language.traditional");
            } else if (languageMode == 3) {
                buttonText = I18n.translate("chineseime.language.cantonese");
            }

            int textWidth = client.textRenderer.getWidth(buttonText);
            int buttonWidth = textWidth + 20;
            int buttonHeight = 20;
            int buttonX = width - buttonWidth - 10;
            int buttonY = 10;

            DrawableHelper.fill(matrices, buttonX, buttonY, buttonX + buttonWidth, buttonY + buttonHeight, 0x80000000);

            int textX = buttonX + (buttonWidth / 2) - (textWidth / 2);
            int textY = buttonY + (buttonHeight / 2) - (client.textRenderer.fontHeight / 2);

            client.textRenderer.drawWithShadow(matrices, buttonText, textX, textY, 0xFFFFFF);
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"))
    private void onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        imeHandler.handleMouseClick((int) mouseX, (int) mouseY);

        MinecraftClient client = MinecraftClient.getInstance();
        int width = client.getWindow().getScaledWidth();

        String buttonText = "";
        int languageMode = ConfigManager.getLanguageMode();
        if (languageMode == 1) {
            buttonText = I18n.translate("chineseime.language.simplified");
        } else if (languageMode == 2) {
            buttonText = I18n.translate("chineseime.language.traditional");
        } else if (languageMode == 3) {
            buttonText = I18n.translate("chineseime.language.cantonese");
        }

        int textWidth = client.textRenderer.getWidth(buttonText);
        int buttonWidth = textWidth + 20;
        int buttonX = width - buttonWidth - 10;
        int buttonY = 10;

        if (mouseX >= buttonX && mouseX <= buttonX + buttonWidth && mouseY >= buttonY && mouseY <= buttonY + 20) {
            imeHandler.toggleLanguageMode();
        }
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"))
    private void onMouseScrolled(double mouseX, double mouseY, double amount, CallbackInfoReturnable<Boolean> cir) {
        imeHandler.handleMouseScroll(amount);
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void onKeyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (imeHandler.isSuggestionListActive()) {
            if (keyCode == GLFW.GLFW_KEY_UP || keyCode == GLFW.GLFW_KEY_DOWN || keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_TAB) {
                imeHandler.handleInput(keyCode);
                cir.setReturnValue(true);
            }
        }
    }
}