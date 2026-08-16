package com.krypton.client.modules.hud;

import com.krypton.client.module.Module;
import com.krypton.client.module.Category;
import com.krypton.client.module.setting.BooleanSetting;

public class Fps extends Module {
    private final BooleanSetting shadow = new BooleanSetting("Shadow", true);

    public Fps() {
        super("FPS", "Shows your frames per second", Category.HUD);
        addSetting(shadow);
    }

    public boolean isShadow() {
        return shadow.get();
    }

    public String getText() {
        return "FPS: " + mc.getCurrentFps();
    }
}