package com.krypton.client.modules.movement;

import com.krypton.client.module.Module;
import com.krypton.client.module.Category;

public class AutoSprint extends Module {
    public AutoSprint() {
        super("AutoSprint", "Sprints automatically when moving forward", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        if (mc.player == null) return;
        if (!mc.player.isTouchingWater() && !mc.player.isSubmergedInWater() && !mc.player.horizontalCollision) {
            mc.player.setSprinting(true);
        }
    }

    @Override
    public String getInfo() {
        return mc.player != null && mc.player.isSprinting() ? "ON" : "OFF";
    }
}