package com.krypton.client.modules.combat;

import com.krypton.client.module.Module;
import com.krypton.client.module.Category;
import com.krypton.client.module.setting.BooleanSetting;
import com.krypton.client.module.setting.NumberSetting;
import net.minecraft.client.option.KeyBinding;

public class AutoClicker extends Module {
    private final NumberSetting cps = new NumberSetting("CPS", 12, 1, 30, 1);
    private final BooleanSetting leftClick = new BooleanSetting("Left Click", true);
    private final BooleanSetting rightClick = new BooleanSetting("Right Click", false);
    private final BooleanSetting onlyInGame = new BooleanSetting("Only In Game", false);

    private long lastClick = 0;

    public AutoClicker() {
        super("AutoClicker", "Automatically clicks for you", Category.COMBAT);
        addSetting(cps);
        addSetting(leftClick);
        addSetting(rightClick);
        addSetting(onlyInGame);
    }

    @Override
    public void onTick() {
        if (onlyInGame.get() && mc.currentScreen != null) return;
        long now = System.currentTimeMillis();
        double interval = 1000.0 / cps.get();
        if (now - lastClick >= interval) {
            lastClick = now;
            if (leftClick.get() && mc.options.attackKey.isPressed()) {
                click(mc.options.attackKey);
            }
            if (rightClick.get() && mc.options.useKey.isPressed()) {
                click(mc.options.useKey);
            }
        }
    }

    private void click(KeyBinding key) {
        KeyBinding.onKeyPressed(key.getDefaultKey());
    }

    @Override
    public String getInfo() {
        return String.valueOf(cps.getInt());
    }
}