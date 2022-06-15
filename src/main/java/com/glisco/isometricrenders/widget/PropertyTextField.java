package com.glisco.isometricrenders.widget;

import com.glisco.isometricrenders.property.IntProperty;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.Objects;
import java.util.function.Predicate;

public class PropertyTextField extends TextFieldWidget {

    private final IntProperty setting;
    private String content = "";

    public PropertyTextField(int x, int y, IntProperty setting) {
        super(MinecraftClient.getInstance().textRenderer, x, y, 35, 20, Text.of(""));
        this.setting = setting;

        this.setText(String.valueOf(setting.get()));
        this.setTextPredicate(makeMatcher());

        this.setting.listen((integerSetting, integer) -> {
            this.setText(String.valueOf(integer));
        });

        this.setChangedListener(s -> {
            if (Objects.equals(s, content) || s.length() < 1 || s.equals("-")) {
                return;
            }

            this.content = s;
            this.setting.set(Integer.parseInt(s));
        });
    }

    private Predicate<String> makeMatcher() {
        final var builder = new StringBuilder();
        if (this.setting.min() < 0) builder.append("-?");

        builder.append("\\d{0,");
        builder.append(String.valueOf(Math.max(Math.abs(this.setting.min()), Math.abs(this.setting.max()))).length());
        builder.append("}");

        final var regex = builder.toString();
        return s -> s.matches(regex);
    }
}
