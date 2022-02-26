package com.glisco.isometricrenders;

import net.minecraft.client.resource.language.I18n;
import net.minecraft.text.BaseText;
import net.minecraft.text.TranslatableText;

public class Translator {
    public static BaseText tr(String key, Object... args) {
        return new TranslatableText(key, args);
    }

    public static String trString(String key, Object... args) {
        return I18n.translate(key, args);
    }
}
