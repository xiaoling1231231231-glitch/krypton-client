package com.krypton.client.modules.hud;

import com.krypton.client.module.Module;
import com.krypton.client.module.Category;
import com.krypton.client.module.setting.BooleanSetting;

public class Coords extends Module {
    private final BooleanSetting nether = new BooleanSetting("Nether coords", true);

    public Coords() {
        super("Coords", "Shows your position and direction", Category.HUD);
        addSetting(nether);
    }

    public String getText() {
        if (mc.player == null) return "XYZ: 0, 0, 0";
        int x = (int) mc.player.getX();
        int y = (int) mc.player.getY();
        int z = (int) mc.player.getZ();
        String dir = getDirection();
        if (nether.get()) {
            int nx = (int) (mc.player.getX() / 8);
            int nz = (int) (mc.player.getZ() / 8);
            return "XYZ: " + x + ", " + y + ", " + z + " (" + nx + ", " + nz + ") " + dir;
        }
        return "XYZ: " + x + ", " + y + ", " + z + " " + dir;
    }

    private String getDirection() {
        String dirs = "N,NE,E,SE,S,SW,W,NW";
        String[] arr = dirs.split(",");
        int dir = (int) Math.floor((mc.player.getYaw() * 8.0F / 360.0F) + 0.5) & 7;
        return arr[dir & 7];
    }
}