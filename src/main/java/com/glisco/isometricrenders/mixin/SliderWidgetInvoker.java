package com.glisco.isometricrenders.mixin;

import net.minecraft.client.gui.widget.SliderWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(SliderWidget.class)
public interface SliderWidgetInvoker {

    @Invoker("setValue")
    void invokeSetValue(double mouseX);

}
