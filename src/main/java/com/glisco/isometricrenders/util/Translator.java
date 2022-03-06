package com.glisco.isometricrenders.util;

import net.minecraft.client.resource.language.I18n;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;

public class Translator {
    private static final String MESSAGE_PREFIX = "message.isometric-renders.prefix";

    public static TranslatableText tr(String key, Object... args) {
        return new TranslatableText(key, args);
    }
    public static TranslatableText gui(String key, Object... args) {
        return tr("gui.isometric-renders." + key, args);
    }
    public static MutableText msg(String key, Object... args) {
        return tr(MESSAGE_PREFIX).append(tr("message.isometric-renders." + key, args));
    }

    public static String trString(String key, Object... args) {
        return I18n.translate(key, args);
    }
}
