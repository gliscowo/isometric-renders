package com.glisco.isometricrenders.widget;

import com.glisco.isometricrenders.property.Property;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.text.Text;

public class PropertyCheckboxComponent extends CheckboxWidget {

    private final Property<Boolean> property;

    public PropertyCheckboxComponent(Text message, Property<Boolean> property) {
        super(0, 0, 20, 20, message, property.get(), false);
        this.property = property;
    }

    @Override
    public void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderButton(context, mouseX, mouseY, delta);
        context.drawText(
                MinecraftClient.getInstance().textRenderer,
                this.getMessage(),
                this.getX() + 24,
                (int) (this.getY() + (this.height - 8) / 2f),
                0xAAAAAA, false
        );
    }

    @Override
    public void onPress() {
        super.onPress();
        property.set(this.isChecked());
    }
}
