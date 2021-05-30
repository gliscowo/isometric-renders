package com.glisco.isometricrenders.client;

import com.glisco.isometricrenders.client.gui.RenderScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v1.ClientCommandManager;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Environment(EnvType.CLIENT)
public class IsometricRendersClient implements ClientModInitializer {

    public static final Logger LOGGER = LogManager.getLogger();
    private static final String PREFIX = "§9[§aIsometric Renders§9]§7 ";

    @Override
    public void onInitializeClient() {

        IsoRenderCommand.register(ClientCommandManager.DISPATCHER);
        ImageExporter.init();

        HudRenderCallback.EVENT.register((matrixStack, tickDelta) -> {
            final MinecraftClient client = MinecraftClient.getInstance();

            if (ImageExporter.getJobCount() < 1 || client.currentScreen != null) return;

            DrawableHelper.fill(matrixStack, 20, 20, 140, 60, 0x90000000);
            client.textRenderer.draw(matrixStack, ImageExporter.getProgressBarText(), 30, 30, 0xFFFFFF);

            RenderScreen.drawExportProgressBar(matrixStack, 30, 45, 100, 50, 10);
        });
    }

    public static Text prefix(String text) {
        return new LiteralText(PREFIX + text);
    }
}
