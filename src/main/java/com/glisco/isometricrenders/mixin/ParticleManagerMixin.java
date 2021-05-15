package com.glisco.isometricrenders.mixin;

import com.glisco.isometricrenders.client.gui.IsometricRenderHelper;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleManager.class)
public class ParticleManagerMixin {

    @Inject(method = "addParticle(Lnet/minecraft/client/particle/Particle;)V", at = @At("HEAD"), cancellable = true)
    public void stopParticles(Particle particle, CallbackInfo ci) {
        if (!IsometricRenderHelper.allowParticles) ci.cancel();
    }
}
