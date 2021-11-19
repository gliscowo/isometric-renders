package com.glisco.isometricrenders.client;

import com.glisco.isometricrenders.client.gui.IsometricRenderPresets;
import com.glisco.isometricrenders.client.gui.IsometricRenderScreen;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;

import static com.glisco.isometricrenders.client.IsometricRendersClient.LOGGER;

public class CommandServer extends WebSocketServer {

    private CommandServer(int port) {
        super(new InetSocketAddress("localhost", port));
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        LOGGER.info("Client '" + conn.getResourceDescriptor() + "' connected");
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        LOGGER.info("Client '" + conn.getResourceDescriptor() + "' disconnected");
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        final var client = MinecraftClient.getInstance();
        if (message.equals("open-screen\n")) {
            RenderSystem.recordRenderCall(() -> client.setScreen(new IsometricRenderScreen()));
            conn.send("ok");
        } else if (message.startsWith("render-item")) {
            var id = new Identifier(message.replace("\n", "").split(" ")[1]);
            if (Registry.ITEM.containsId(id)) {
                RenderSystem.recordRenderCall(() -> {
                    var screen = new IsometricRenderScreen();
                    final var stack = new ItemStack(Registry.ITEM.get(id));

                    setupItem(client.getItemRenderer().getModel(stack, null, null, 0).hasDepth());
                    IsometricRenderPresets.setupItemStackRender(screen, stack);

                    screen.setExportCallback(file -> conn.send("exported:" + id + ":" + file.getAbsolutePath()));
                    screen.scheduleCapture();
                    client.setScreen(screen);
                });
                conn.send("ok");
            } else {
                conn.send("error:unknown item");
            }
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        LOGGER.warn("Exception thrown on command server", ex);
    }

    @Override
    public void onStart() {
        LOGGER.info("WebSocket command server started");
    }

    private static CommandServer INSTANCE = null;

    public static boolean open(int port) {
        if (running()) return false;

        INSTANCE = new CommandServer(port);
        INSTANCE.start();
        return true;
    }

    public static boolean close() {
        if (!running()) return false;

        try {
            INSTANCE.stop();
            INSTANCE = null;
            return true;
        } catch (InterruptedException e) {
            LOGGER.warn("Could not stop command server", e);
            return false;
        }
    }

    public static boolean running() {
        return INSTANCE != null;
    }

    private static void setupItem(boolean depth) {
        RuntimeConfig.angle = depth ? 30 : 0;
        RuntimeConfig.rotation = depth ? 225 : 0;

        RuntimeConfig.renderScale = 240;
        RuntimeConfig.exportResolution = 64;
        RuntimeConfig.dumpIntoRoot = true;
    }
}
