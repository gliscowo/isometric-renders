package com.glisco.isometricrenders.mixin.angerables;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.Angerable;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.PolarBearEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PolarBearEntity.class)
public abstract class PolarBearEntityMixin extends MobEntity implements Angerable {

    private PolarBearEntityMixin(EntityType<? extends MobEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(
            method = "readCustomDataFromTag",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/passive/AnimalEntity;readCustomDataFromTag(Lnet/minecraft/nbt/CompoundTag;)V", shift = At.Shift.AFTER),
            cancellable = true
    )
    private void worldCheckAngerFromTag(CompoundTag tag, CallbackInfo ci) {
        if(!this.world.isClient) {
            this.angerFromTag((ServerWorld) world, tag);
        }

        ci.cancel();
    }
}
