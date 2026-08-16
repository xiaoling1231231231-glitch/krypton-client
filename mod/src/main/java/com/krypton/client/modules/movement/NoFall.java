package com.krypton.client.modules.movement;

import com.krypton.client.module.Module;
import com.krypton.client.module.Category;
import com.krypton.client.module.setting.BooleanSetting;

public class NoFall extends Module {
    private final BooleanSetting smart = new BooleanSetting("Smart", true);

    public NoFall() {
        super("NoFall", "Prevents fall damage", Category.MOVEMENT);
        addSetting(smart);
    }

    @Override
    public void onTick() {
        if (mc.player == null) return;
        if (mc.player.fallDistance > 3.0f && !mc.player.isOnGround()) {
            if (smart.get() && mc.player.getVelocity().y >= 0) return;
            mc.player.setOnGround(true);
            mc.player.fallDistance = 0;
        }
    }
}