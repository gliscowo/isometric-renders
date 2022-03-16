package com.glisco.isometricrenders.util;

import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v1.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;

public class Translate {

    private static final String MESSAGE_PREFIX = "message.isometric-renders.prefix";

    public static TranslatableText make(String key, Object... args) {
        return new TranslatableText("message.isometric-renders." + key, args);
    }

    public static TranslatableText gui(String key, Object... args) {
        return new TranslatableText("gui.isometric-renders." + key, args);
    }

    public static MutableText msg(String key, Object... args) {
        return new TranslatableText(MESSAGE_PREFIX).append(make(key, args).formatted(Formatting.GRAY));
    }

    public static void commandFeedback(CommandContext<FabricClientCommandSource> context, String key, Object... args) {
        context.getSource().sendFeedback(msg(key, args));
    }

    public static void commandError(CommandContext<FabricClientCommandSource> context, String key, Object... args) {
        context.getSource().sendError(msg(key, args));
    }

    public static void chat(String key, Object... args) {
        MinecraftClient.getInstance().player.sendMessage(msg(key, args), false);
    }

    public static void actionBar(String key, Object... args) {
        MinecraftClient.getInstance().player.sendMessage(make(key, args), true);
    }
}
