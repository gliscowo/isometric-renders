package com.glisco.isometricrenders.mixin.access;

import net.minecraft.client.gl.Framebuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Framebuffer.class)
public interface FramebufferAccessor {

    @Accessor("depthAttachment")
    void isometric$setDepthAttachment(int depthAttachment);

    @Accessor("fbo")
    void isometric$setFbo(int fbo);

    @Accessor("fbo")
    int isometric$getFbo();

}
