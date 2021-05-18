package com.glisco.isometricrenders.mixin.angerables;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.Angerable;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EndermanEntity.class)
public abstract class EndermanEntityMixin extends MobEntity implements Angerable {

    private EndermanEntityMixin(EntityType<? extends MobEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(
            method = "readCustomDataFromTag",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/mob/EndermanEntity;setCarriedBlock(Lnet/minecraft/block/BlockState;)V", shift = At.Shift.AFTER),
            cancellable = true
    )
    private void worldCheckAngerFromTag(CompoundTag tag, CallbackInfo ci) {
        if (!this.world.isClient) {
            this.angerFromTag((ServerWorld) world, tag);
        }

        ci.cancel();
    }
}
