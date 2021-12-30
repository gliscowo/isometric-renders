package com.glisco.isometricrenders.util;

import com.glisco.isometricrenders.render.IsometricRenderPresets;
import com.glisco.isometricrenders.screen.IsometricRenderScreen;
import com.mojang.blaze3d.systems.RenderSystem;
import io.wispforest.exo.api.Exo;
import io.wispforest.exo.api.ExoCommandChannel;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class IsometricChannel extends ExoCommandChannel {

    public IsometricChannel() {
        addCommand("open-screen", (port, arguments) -> {
            RenderSystem.recordRenderCall(() -> MinecraftClient.getInstance().setScreen(new IsometricRenderScreen()));
            return Exo.OK_RESPONSE;
        });

        addCommand("render-item", (port, arguments) -> {
            if (arguments.length < 1) return Exo.join("error", "missing item id");

            final var id = new Identifier(arguments[0]);
            final var client = MinecraftClient.getInstance();

            if (Registry.ITEM.containsId(id)) {
                MinecraftClient.getInstance().execute(() -> {
                    var screen = new IsometricRenderScreen();
                    final var stack = new ItemStack(Registry.ITEM.get(id));

                    setupItem(client.getItemRenderer().getModel(stack, null, null, 0).hasDepth());
                    IsometricRenderPresets.setupItemStackRender(screen, stack);

                    screen.setExportCallback(file -> send("exported#" + id + "#" + file.getAbsolutePath(), port));
                    screen.setExportCallback(file -> send(Exo.join("exported", id.toString(), file.getAbsolutePath()), port));
                    screen.scheduleCapture();
                    client.setScreen(screen);
                });
                return Exo.OK_RESPONSE;
            } else {
                return Exo.join("error", "unknown item");
            }
        });
    }

    private static void setupItem(boolean depth) {
        RuntimeConfig.angle = depth ? 30 : 0;
        RuntimeConfig.rotation = depth ? 225 : 0;

        RuntimeConfig.renderScale = depth ? 310 : 235;
        RuntimeConfig.exportResolution = 32;
        RuntimeConfig.dumpIntoRoot = true;
    }

    @Override
    public String getId() {
        return "isometric";
    }
}
