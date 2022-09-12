package com.glisco.isometricrenders.util;

import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;

public class Translate {

    public static final Text PREFIX = generatePrefix("Isometric Renders", 190, 155);

    public static MutableText make(String key, Object... args) {
        return Text.translatable("message.isometric-renders." + key, args);
    }

    public static MutableText gui(String key, Object... args) {
        return Text.translatable("gui.isometric-renders." + key, args);
    }

    public static MutableText msg(String key, Object... args) {
        return prefixed(make(key, args).formatted(Formatting.GRAY));
    }

    public static void commandFeedback(CommandContext<FabricClientCommandSource> context, String key, Object... args) {
        context.getSource().sendFeedback(msg(key, args));
    }

    public static void commandError(CommandContext<FabricClientCommandSource> context, String key, Object... args) {
        context.getSource().sendError(msg(key, args));
    }

    public static MutableText prefixed(Text text) {
        return Text.empty()
                .append(PREFIX)
                .append(Text.literal(" > ").formatted(Formatting.DARK_GRAY))
                .append(text);
    }

    @SuppressWarnings("SameParameterValue")
    private static Text generatePrefix(String text, int startHue, int endHue) {
        int hueSpan = endHue - startHue;
        char[] chars = text.toCharArray();

        var prefixText = Text.empty();

        for (int i = 0; i < chars.length; i++) {
            float index = i;
            prefixText.append(Text.literal(String.valueOf(chars[i])).styled(style ->
                    style.withColor(MathHelper.hsvToRgb((startHue + (index / chars.length) * hueSpan) / 360, 1, 0.96f))
            ));
        }

        return prefixText;
    }

    public static void actionBar(String key, Object... args) {
        MinecraftClient.getInstance().player.sendMessage(make(key, args), true);
    }
}
