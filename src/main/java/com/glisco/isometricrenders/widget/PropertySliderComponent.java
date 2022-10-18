package com.glisco.isometricrenders.widget;

import com.glisco.isometricrenders.mixin.SliderWidgetInvoker;
import com.glisco.isometricrenders.property.IntProperty;
import io.wispforest.owo.ui.component.SliderComponent;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.text.Text;

public class PropertySliderComponent extends SliderComponent {

    private final IntProperty setting;
    private final int scrollIncrement;

    public PropertySliderComponent(Sizing horizontalSizing, Text text, int scrollIncrement, IntProperty setting) {
        super(horizontalSizing);
        this.setting = setting;
        this.scrollIncrement = scrollIncrement;

        this.message(s -> text);

        this.onChanged(this.setting::setFromProgress);
        setting.listen((intSetting, integer) -> ((SliderWidgetInvoker) this).isometric$setValue(setting.progress()));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 2) {
            this.setting.setToDefault();
            return true;
        } else {
            return super.mouseClicked(mouseX, mouseY, button);
        }
    }

    @Override
    public boolean onMouseScroll(double mouseX, double mouseY, double amount) {
        this.setting.modify((int) Math.round(amount * this.scrollIncrement));
        return true;
    }
}
