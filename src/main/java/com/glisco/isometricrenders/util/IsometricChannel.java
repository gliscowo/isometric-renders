package com.glisco.isometricrenders.util;

import com.glisco.isometricrenders.render.IsometricRenderPresets;
import com.glisco.isometricrenders.screen.IsometricRenderScreen;
import com.glisco.isometricrenders.setting.Settings;
import io.wispforest.exo.api.Exo;
import io.wispforest.exo.api.ExoCommandChannel;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class IsometricChannel extends ExoCommandChannel {

    public IsometricChannel() {
        addCommand("open-screen", (port, arguments) -> {
            MinecraftClient.getInstance().execute(() -> MinecraftClient.getInstance().setScreen(new IsometricRenderScreen()));
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
        Settings.angle.set(depth ? 30 : 0);
        Settings.rotation.set(depth ? 225 : 0);

        Settings.renderScale.set(depth ? 310 : 235);
        Settings.exportResolution = 32;
        Settings.dumpIntoRoot.set(true);
    }

    @Override
    public String getId() {
        return "isometric";
    }
}
