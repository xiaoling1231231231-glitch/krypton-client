package com.krypton.client.mixin;

import com.krypton.client.KryptonClient;
import com.krypton.client.modules.combat.Reach;
import com.krypton.client.modules.player.HitBox;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {
    @Inject(method = "getEntityInteractionRange", at = @At("RETURN"), cancellable = true)
    private void krypton$extendReach(CallbackInfoReturnable<Double> cir) {
        KryptonClient.MODULE_MANAGER.get("Reach").ifPresent(m -> {
            if (m.isEnabled()) {
                cir.setReturnValue(cir.getReturnValue() + ((Reach) m).getReach() - 3.0);
            }
        });
    }

    @Inject(method = "getBlockInteractionRange", at = @At("RETURN"), cancellable = true)
    private void krypton$extendBlockReach(CallbackInfoReturnable<Double> cir) {
        KryptonClient.MODULE_MANAGER.get("Reach").ifPresent(m -> {
            if (m.isEnabled()) {
                cir.setReturnValue(cir.getReturnValue() + ((Reach) m).getReach() - 3.0);
            }
        });
    }

    @Inject(method = "getDimensions", at = @At("RETURN"), cancellable = true)
    private void krypton$extendHitbox(EntityPose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        KryptonClient.MODULE_MANAGER.get("HitBox").ifPresent(m -> {
            if (m.isEnabled()) {
                float expand = ((HitBox) m).getExpand();
                cir.setReturnValue(cir.getReturnValue().scaled(1.0f + expand));
            }
        });
    }
}