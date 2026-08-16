package com.krypton.client.modules.render;

import com.krypton.client.module.Module;
import com.krypton.client.module.Category;

public class NoHurtCam extends Module {
    public NoHurtCam() {
        super("NoHurtCam", "Removes the screen shake when hurt", Category.RENDER);
    }

    @Override
    public String getInfo() {
        return null;
    }
}