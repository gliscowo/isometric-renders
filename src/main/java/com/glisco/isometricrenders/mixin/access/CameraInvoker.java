package com.glisco.isometricrenders.mixin.access;

import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Camera.class)
public interface CameraInvoker {

    @Invoker
    void invokeSetRotation(float yaw, float pitch);

}
