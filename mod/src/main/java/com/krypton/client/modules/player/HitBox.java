package com.krypton.client.modules.player;

import com.krypton.client.module.Module;
import com.krypton.client.module.Category;
import com.krypton.client.module.setting.NumberSetting;

public class HitBox extends Module {
    private final NumberSetting size = new NumberSetting("Size", 0.4, 0.05, 1.5, 0.05);

    public HitBox() {
        super("HitBox", "Expands entity hitboxes", Category.COMBAT);
        addSetting(size);
    }

    public float getExpand() {
        return size.getFloat();
    }
}