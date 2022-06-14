package com.glisco.isometricrenders.mixin;

import com.glisco.isometricrenders.IsometricRenders;
import com.glisco.isometricrenders.screen.RenderScreen;
import com.glisco.isometricrenders.util.ParticleRestriction;
import net.minecraft.client.MinecraftClient;
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
        if (!(MinecraftClient.getInstance().currentScreen instanceof RenderScreen screen)) return;
        if (!screen.tickParticles.get()) {
            ci.cancel();
            return;
        }

        final var restriction = IsometricRenders.particleRestriction;

        if (restriction.is(ParticleRestriction.ALLOW_NEVER)) {
            return;
        }

        if (restriction.is(ParticleRestriction.ALLOW_DURING_TICK)) {
            if (!restriction.conditionFor(ParticleRestriction.ALLOW_DURING_TICK).get()) {
                ci.cancel();
            }
        } else if (restriction.is(ParticleRestriction.ALLOW_IN_AREA)) {
            if (!restriction.conditionFor(ParticleRestriction.ALLOW_IN_AREA).test(particle.getBoundingBox())) {
                ci.cancel();
            }
        }
    }
}
