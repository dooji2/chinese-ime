package com.dooji.chineseime;

import com.dooji.chineseime.processing.ConfigManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class ChineseIME implements ModInitializer {

	private IMEHandler imeHandler;

	@Override
	public void onInitialize() {
		ConfigManager.init();

		MinecraftClient client = MinecraftClient.getInstance();
		imeHandler = new IMEHandler(client);

		ClientTickEvents.END_CLIENT_TICK.register(minecraftClient -> {
			if (minecraftClient.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen) {
				imeHandler.updateSuggestionsBasedOnInput();
			}
		});

		ClientTickEvents.END_CLIENT_TICK.register(minecraftClient -> {
			if (minecraftClient.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen) {
				if (InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), GLFW.GLFW_KEY_UP)) {
					imeHandler.handleInput(GLFW.GLFW_KEY_UP);
				}
				if (InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), GLFW.GLFW_KEY_DOWN)) {
					imeHandler.handleInput(GLFW.GLFW_KEY_DOWN);
				}
				if (InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), GLFW.GLFW_KEY_ENTER)) {
					imeHandler.handleInput(GLFW.GLFW_KEY_ENTER);
				}
				if (InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), GLFW.GLFW_KEY_TAB)) {
					imeHandler.handleInput(GLFW.GLFW_KEY_TAB);
				}
			}
		});
	}
}