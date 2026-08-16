package com.krypton.client.modules.render;

import com.krypton.client.KryptonClient;
import com.krypton.client.module.Module;
import com.krypton.client.module.Category;
import com.krypton.client.module.setting.BooleanSetting;
import com.krypton.client.module.setting.NumberSetting;
import net.minecraft.client.option.KeyBinding;

public class Zoom extends Module {
    private final NumberSetting level = new NumberSetting("Level", 4, 2, 12, 1);
    private final BooleanSetting smooth = new BooleanSetting("Smooth", true);
    private int lastFov = -1;
    private double zoomFov;
    private boolean wasPressed = false;

    public Zoom() {
        super("Zoom", "Zooms in while holding the zoom key", Category.RENDER);
        addSetting(level);
        addSetting(smooth);
    }

    @Override
    public void onTick() {
        KeyBinding zoomKey = KryptonClient.zoomKey;
        boolean pressed = zoomKey != null && zoomKey.isPressed();
        if (pressed && !wasPressed) {
            if (lastFov == -1) {
                lastFov = mc.options.getFov().getValue();
            }
            wasPressed = true;
        }
        if (!pressed && wasPressed) {
            wasPressed = false;
        }
        if (pressed) {
            if (lastFov == -1) lastFov = mc.options.getFov().getValue();
            double target = lastFov / level.get();
            if (smooth.get()) {
                zoomFov += (target - zoomFov) * 0.3;
            } else {
                zoomFov = target;
            }
            mc.options.getFov().setValue((int) Math.round(zoomFov));
        }
    }

    @Override
    public void onTickAlways() {
        if (!isEnabled() && lastFov != -1) {
            mc.options.getFov().setValue(lastFov);
            lastFov = -1;
        }
    }

    @Override
    protected void onDisable() {
        if (lastFov != -1) {
            mc.options.getFov().setValue(lastFov);
            lastFov = -1;
        }
        wasPressed = false;
    }
}