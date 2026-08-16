package com.krypton.client.modules.combat;

import com.krypton.client.module.Module;
import com.krypton.client.module.Category;
import com.krypton.client.module.setting.NumberSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

public class TriggerBot extends Module {
    private final NumberSetting range = new NumberSetting("Range", 4, 1, 8, 0.5);

    public TriggerBot() {
        super("TriggerBot", "Attacks when your crosshair is on an entity", Category.COMBAT);
        addSetting(range);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;
        if (mc.crosshairTarget == null) return;
        if (mc.crosshairTarget.getType() != HitResult.Type.ENTITY) return;
        EntityHitResult hit = (EntityHitResult) mc.crosshairTarget;
        Entity e = hit.getEntity();
        if (!(e instanceof LivingEntity living)) return;
        if (living.isDead()) return;
        if (mc.player.squaredDistanceTo(e) > range.get() * range.get()) return;
        if (mc.player.getAttackCooldownProgress(0) >= 1.0f) {
            mc.interactionManager.attackEntity(mc.player, e);
            mc.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
        }
    }
}