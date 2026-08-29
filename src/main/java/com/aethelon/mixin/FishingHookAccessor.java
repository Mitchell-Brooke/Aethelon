package com.aethelon.mixin;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.projectile.FishingHook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FishingHook.class)
public interface FishingHookAccessor {
    @Accessor("DATA_BITING")
    static EntityDataAccessor<Boolean> aethelon$getDataBiting() {
        throw new AssertionError();
    }
}