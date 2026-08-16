package com.krypton.client.modules.hud;

import com.krypton.client.module.Module;
import com.krypton.client.module.Category;
import com.krypton.client.module.setting.BooleanSetting;
import com.krypton.client.module.setting.ModeSetting;

public class Watermark extends Module {
    private final ModeSetting style = new ModeSetting("Style", "Krypton", "Krypton", "Client", "Hidden");
    private final BooleanSetting shadow = new BooleanSetting("Shadow", true);

    public Watermark() {
        super("Watermark", "Shows the client watermark on screen", Category.HUD);
        addSetting(style);
        addSetting(shadow);
    }

    public boolean isShadow() {
        return shadow.get();
    }

    public String getText() {
        return "§9Krypton §fClient";
    }
}