package com.krypton.client.modules.render;

import com.krypton.client.module.Module;
import com.krypton.client.module.Category;
import com.krypton.client.module.setting.NumberSetting;

public class FullBright extends Module {
    private final NumberSetting gamma = new NumberSetting("Gamma", 5, 1, 15, 0.5);
    private double lastGamma = -1;

    public FullBright() {
        super("FullBright", "Brightens the world", Category.RENDER);
        addSetting(gamma);
    }

    @Override
    public void onTick() {
        if (lastGamma == -1) lastGamma = mc.options.getGamma().getValue();
        mc.options.getGamma().setValue(gamma.get());
    }

    @Override
    public void onTickAlways() {
        if (!isEnabled() && lastGamma != -1) {
            mc.options.getGamma().setValue(lastGamma);
            lastGamma = -1;
        }
    }

    @Override
    protected void onDisable() {
        if (lastGamma != -1) {
            mc.options.getGamma().setValue(lastGamma);
            lastGamma = -1;
        }
    }
}