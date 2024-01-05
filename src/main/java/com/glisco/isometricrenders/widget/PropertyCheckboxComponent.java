package com.glisco.isometricrenders.widget;

import com.glisco.isometricrenders.property.Property;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.text.Text;

public class PropertyCheckboxComponent extends CheckboxWidget {

    private final Property<Boolean> property;

    public PropertyCheckboxComponent(Text message, Property<Boolean> property) {
        super(0, 0, message, MinecraftClient.getInstance().textRenderer, property.get(), (checkbox, checked) -> {});
        this.property = property;
    }

    @Override
    public void onPress() {
        super.onPress();
        property.set(this.isChecked());
    }
}
