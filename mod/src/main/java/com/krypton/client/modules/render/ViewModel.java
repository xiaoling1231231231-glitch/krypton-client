package com.krypton.client.modules.render;

import com.krypton.client.module.Module;
import com.krypton.client.module.Category;
import com.krypton.client.module.setting.BooleanSetting;
import com.krypton.client.module.setting.NumberSetting;

public class ViewModel extends Module {
    public final NumberSetting scale = new NumberSetting("Scale", 1, 0.1, 2, 0.05);

    public ViewModel() {
        super("ViewModel", "Adjusts your held item view", Category.RENDER);
        addSetting(scale);
    }

    public float getScale() {
        return scale.getFloat();
    }
}