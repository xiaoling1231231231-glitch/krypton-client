package com.krypton.client.modules.combat;

import com.krypton.client.module.Module;
import com.krypton.client.module.Category;
import com.krypton.client.module.setting.NumberSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class AimAssist extends Module {
    private final NumberSetting range = new NumberSetting("Range", 4, 1, 8, 0.5);
    private final NumberSetting strength = new NumberSetting("Strength", 80, 1, 100, 1);
    private final NumberSetting speed = new NumberSetting("Speed", 12, 1, 40, 1);

    public AimAssist() {
        super("AimAssist", "Gently moves your aim toward nearby entities", Category.COMBAT);
        addSetting(range);
        addSetting(strength);
        addSetting(speed);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;
        if (mc.player.getAttackCooldownProgress(0) < 1) return;
        Entity target = findTarget();
        if (target == null) return;

        Vec3d playerPos = mc.player.getEyePos();
        Vec3d targetPos = target.getBoundingBox().getCenter();
        double deltaX = targetPos.x - playerPos.x;
        double deltaY = targetPos.y - playerPos.y;
        double deltaZ = targetPos.z - playerPos.z;

        double distance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        float yaw = (float) Math.toDegrees(Math.atan2(-deltaX, deltaZ));
        float pitch = (float) Math.toDegrees(Math.atan2(deltaY, distance));

        yaw = correctAngle(yaw - mc.player.getYaw());
        pitch = correctAngle(pitch - mc.player.getPitch());

        float multiplier = speed.getFloat() * 0.05f * (strength.getFloat() / 100f);
        mc.player.setYaw(mc.player.getYaw() + yaw * multiplier);
        mc.player.setPitch(mc.player.getPitch() + pitch * multiplier);
    }

    private Entity findTarget() {
        double closest = range.get();
        Entity target = null;
        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof LivingEntity living)) continue;
            if (e == mc.player) continue;
            if (living.isDead()) continue;
            double dist = mc.player.squaredDistanceTo(e);
            if (dist < closest * closest && mc.player.canSee(e)) {
                closest = Math.sqrt(dist);
                target = e;
            }
        }
        return target;
    }

    private float correctAngle(float angle) {
        angle %= 360f;
        if (angle > 180f) angle -= 360f;
        if (angle < -180f) angle += 360f;
        return angle;
    }
}