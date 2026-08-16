package com.krypton.client.modules.movement;

import com.krypton.client.module.Module;
import com.krypton.client.module.Category;
import com.krypton.client.module.setting.BooleanSetting;

public class Sprint extends Module {
    private final BooleanSetting multiplayer = new BooleanSetting("Multiplayer", true);

    public Sprint() {
        super("Sprint", "Sprints automatically when moving forward (server-side safe)", Category.MOVEMENT);
        addSetting(multiplayer);
    }

    @Override
    public void onTick() {
        if (mc.player == null) return;
        if (mc.isIntegratedServerRunning() || multiplayer.get()) {
            if (mc.player.forwardSpeed > 0 && !mc.player.horizontalCollision
                    && !mc.player.isTouchingWater() && !mc.player.isSubmergedInWater()) {
                mc.player.setSprinting(true);
            }
        }
    }

    @Override
    public String getInfo() {
        return mc.player != null && mc.player.isSprinting() ? "ON" : "OFF";
    }
}