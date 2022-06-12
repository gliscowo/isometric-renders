package com.glisco.isometricrenders.util;

import com.glisco.isometricrenders.render.ItemRenderable;
import com.glisco.isometricrenders.render.Renderable;
import com.glisco.isometricrenders.screen.RenderScreen;
import io.wispforest.exo.api.Exo;
import io.wispforest.exo.api.ExoCommandChannel;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class IsometricChannel extends ExoCommandChannel {

    public IsometricChannel() {
        addCommand("open-screen", (port, arguments) -> {
            MinecraftClient.getInstance().execute(() -> MinecraftClient.getInstance().setScreen(new RenderScreen(Renderable.EMPTY)));
            return Exo.OK_RESPONSE;
        });

        addCommand("render-item", (port, arguments) -> {
            if (arguments.length < 1) return Exo.join("error", "missing item id");

            final var id = new Identifier(arguments[0]);
            final var client = MinecraftClient.getInstance();

            if (Registry.ITEM.containsId(id)) {
                MinecraftClient.getInstance().execute(() -> {
                    final var stack = new ItemStack(Registry.ITEM.get(id));
                    final var renderable = new ItemRenderable(stack);

                    var screen = new RenderScreen(renderable);
                    setupItem(renderable, client.getItemRenderer().getModel(stack, null, null, 0).hasDepth());

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

    private static void setupItem(ItemRenderable renderable, boolean depth) {
        renderable.properties().slant.set(depth ? 30 : 0);
        renderable.properties().rotation.set(depth ? 225 : 0);

        renderable.properties().scale.set(depth ? 310 : 235);
//        renderable.properties().exportResolution = 32;
//        renderable.properties().dumpIntoRoot.set(true);
    }

    @Override
    public String getId() {
        return "isometric";
    }
}
