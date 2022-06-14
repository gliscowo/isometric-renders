package com.glisco.isometricrenders.widget;

import com.glisco.isometricrenders.mixin.access.ButtonWidgetAccessor;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.List;
import java.util.function.Supplier;

public class IsometricButtonWidget extends ButtonWidget {
    public IsometricButtonWidget(int x, int y, int width, int height, Text message, PressAction onPress) {
        super(x, y, width, height, message, onPress);
    }

    public IsometricButtonWidget withTooltip(Screen screen, Supplier<List<Text>> tooltip) {
        ((ButtonWidgetAccessor) this).isometric$setTooltipSupplier((button, matrices, mouseX, mouseY) -> {
            screen.renderTooltip(matrices, tooltip.get(), mouseX, mouseY);
        });

        return this;
    }
}
