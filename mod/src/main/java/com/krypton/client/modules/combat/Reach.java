package com.krypton.client.modules.combat;

import com.krypton.client.module.Module;
import com.krypton.client.module.Category;
import com.krypton.client.module.setting.NumberSetting;

public class Reach extends Module {
    private final NumberSetting distance = new NumberSetting("Distance", 3.5, 3, 6, 0.1);

    public Reach() {
        super("Reach", "Extends your attack range", Category.COMBAT);
        addSetting(distance);
    }

    public double getReach() {
        return distance.get();
    }

    @Override
    public String getInfo() {
        return distance.getDisplayValue();
    }
}