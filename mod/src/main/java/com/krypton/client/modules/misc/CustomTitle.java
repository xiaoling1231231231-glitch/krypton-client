package com.krypton.client.modules.misc;

import com.krypton.client.module.Module;
import com.krypton.client.module.Category;
import com.krypton.client.module.setting.ModeSetting;
import com.krypton.client.module.setting.StringSetting;

public class CustomTitle extends Module {
    public final StringSetting title = new StringSetting("Title", "KRYPTON CLIENT", 48);
    public final ModeSetting palette = new ModeSetting("Colors",
            "Trans",
            "Default", "Trans", "Gay", "Pansexual", "Lesbian", "Bisexual",
            "Nonbinary", "Asexual", "Aromantic", "Genderfluid");

    public CustomTitle() {
        super("CustomTitle", "Replaces the Minecraft title with custom gradient text", Category.MISC);
        addSetting(title);
        addSetting(palette);
    }

    public String getTitle() {
        return title.get();
    }

    // 0xRRGGBB palettes for each flag theme
    public int[] getPalette() {
        switch (palette.get()) {
            case "Trans":
                return new int[]{0x55CDFC, 0x55CDFC, 0xF7A8B8, 0xF7A8B8, 0xFFFFFF, 0xFFFFFF, 0xF7A8B8, 0xF7A8B8, 0x55CDFC, 0x55CDFC};
            case "Gay":
                return new int[]{0xFF0000, 0xFF7700, 0xFFFF00, 0x00FF66, 0x00FFFF, 0x6633FF, 0xCC33FF};
            case "Pansexual":
                return new int[]{0xFF218C, 0xFF218C, 0xFFD800, 0xFFD800, 0x21B1FF, 0x21B1FF, 0xFFD800, 0xFFD800, 0xFF218C, 0xFF218C};
            case "Lesbian":
                return new int[]{0xD62800, 0xD62800, 0xFF9B56, 0xFFFFFF, 0xD462A6, 0xD462A6, 0xA50062, 0xA50062};
            case "Bisexual":
                return new int[]{0xD60270, 0xD60270, 0xD60270, 0x9B4F96, 0x0038A8, 0x0038A8, 0x0038A8};
            case "Nonbinary":
                return new int[]{0xFFD800, 0xFFD800, 0xFFD800, 0x9C59D1, 0xFFFFFF, 0xFFFFFF, 0x2C2C2C, 0x2C2C2C, 0x2C2C2C};
            case "Asexual":
                return new int[]{0x000000, 0x000000, 0xA4A4A4, 0xA4A4A4, 0xFFFFFF, 0xFFFFFF, 0x810081, 0x810081};
            case "Aromantic":
                return new int[]{0x3DA542, 0x3DA542, 0xA7D379, 0xFFFFFF, 0xA9A9A9, 0xFFFFFF, 0x000000, 0x000000};
            case "Genderfluid":
                return new int[]{0xFF76A4, 0xFF76A4, 0xFFFFFF, 0xC011D7, 0x000000, 0x3333BE, 0x3333BE};
            case "Default":
            default:
                return new int[]{0x9A9A9A, 0xA0A0A0, 0xA8A8A8, 0xB0B0B0, 0xB8B8B8, 0xC0C0C0};
        }
    }
}