package com.glisco.isometricrenders.widget;

import com.glisco.isometricrenders.setting.Setting;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.text.Text;

public class SettingCheckbox extends CheckboxWidget {

    private final Setting<Boolean> setting;

    public SettingCheckbox(int x, int y, Text message, Setting<Boolean> setting) {
        super(x, y, 20, 20, message, setting.get());
        this.setting = setting;
    }

    @Override
    public void onPress() {
        super.onPress();
        setting.set(isChecked());
    }
}
