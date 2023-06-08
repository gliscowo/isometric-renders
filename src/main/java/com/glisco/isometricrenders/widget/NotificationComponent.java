package com.glisco.isometricrenders.widget;

import com.glisco.isometricrenders.util.Translate;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import io.wispforest.owo.ui.util.UISounds;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

public class NotificationComponent extends FlowLayout {

    private float age = 0;

    public NotificationComponent(@Nullable Runnable onClick, Text... messages) {
        super(Sizing.content(), Sizing.content(), Algorithm.VERTICAL);

        this.margins(Insets.top(5));
        this.padding(Insets.of(10));
        this.surface(Surface.flat(0x77000000).and(Surface.outline(0x77000000)));

        if (onClick != null) {
            this.cursorStyle(CursorStyle.HAND);
            this.mouseDown().subscribe((mouseX, mouseY, button) -> {
                if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;

                onClick.run();
                UISounds.playInteractionSound();

                return true;
            });
        }

        for (var message : messages) {
            final var label = Components.label(message);
            if (onClick != null) {
                label.tooltip(Translate.gui("click_to_open")).cursorStyle(this.cursorStyle);
            }

            this.child(label);
        }
    }

    @Override
    protected void parentUpdate(float delta, int mouseX, int mouseY) {
        super.parentUpdate(delta, mouseX, mouseY);
        this.age += delta * 50;

        if (this.age > 5000 && this.horizontalSizing.get().method == Sizing.Method.CONTENT) {
            this.verticalSizing(Sizing.fixed(this.height));
            this.horizontalSizing(Sizing.fixed(this.width));

            this.margins.animate(1500, Easing.CUBIC, Insets.none()).forwards();
            this.verticalSizing.animate(1500, Easing.CUBIC, Sizing.fixed(0)).forwards();
            this.horizontalSizing.animate(1500, Easing.CUBIC, Sizing.fixed(0)).forwards();
        }

        if (this.age > 6500) {
            this.queue(() -> this.parent.removeChild(this));
        }
    }
}
