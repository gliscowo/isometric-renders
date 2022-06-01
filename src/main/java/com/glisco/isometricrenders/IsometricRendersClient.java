package com.glisco.isometricrenders;

import com.glisco.isometricrenders.screen.RenderScreen;
import com.glisco.isometricrenders.util.AreaSelectionHelper;
import com.glisco.isometricrenders.util.ImageExporter;
import com.glisco.isometricrenders.util.IsoRenderCommand;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.util.registry.DynamicRegistryManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;


@Environment(EnvType.CLIENT)
public class IsometricRendersClient implements ClientModInitializer {

    public static final Logger LOGGER = LogManager.getLogger();

    private static final KeyBinding SELECT = new KeyBinding("key.isometric-renders.area_select", GLFW.GLFW_KEY_C, KeyBinding.MISC_CATEGORY);

    @Override
    public void onInitializeClient() {
        final var registryAccess = new CommandRegistryAccess(DynamicRegistryManager.BUILTIN.get());
        IsoRenderCommand.register(ClientCommandManager.DISPATCHER, registryAccess);

        ImageExporter.init();

        HudRenderCallback.EVENT.register((matrixStack, tickDelta) -> {
            final MinecraftClient client = MinecraftClient.getInstance();

            if (ImageExporter.getJobCount() < 1 || client.currentScreen != null) return;

            DrawableHelper.fill(matrixStack, 20, 20, 140, 60, 0x90000000);
            client.textRenderer.draw(matrixStack, ImageExporter.getProgressBarText(), 30, 30, 0xFFFFFF);

            RenderScreen.drawExportProgressBar(matrixStack, 30, 45, 100, 50, 10);
        });

        KeyBindingHelper.registerKeyBinding(SELECT);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!SELECT.wasPressed()) return;

            if (client.player.isSneaking()) {
                AreaSelectionHelper.clear();
            } else {
                AreaSelectionHelper.select();
            }
        });
    }
}
