package com.krypton.client.modules.hud;

import com.krypton.client.module.Module;
import com.krypton.client.module.Category;
import com.krypton.client.module.setting.BooleanSetting;

public class SessionInfo extends Module {
    private final BooleanSetting serverIp = new BooleanSetting("Server IP", true);
    private final BooleanSetting ping = new BooleanSetting("Ping", true);

    public SessionInfo() {
        super("Session Info", "Shows connection information", Category.HUD);
        addSetting(serverIp);
        addSetting(ping);
    }

    public String getText() {
        StringBuilder sb = new StringBuilder();
        if (serverIp.get()) {
            if (mc.isInSingleplayer()) {
                sb.append("Singleplayer");
            } else if (mc.getCurrentServerEntry() != null) {
                sb.append(mc.getCurrentServerEntry().address);
            } else {
                sb.append("Not connected");
            }
        }
        if (ping.get() && mc.getNetworkHandler() != null) {
            int p = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid()) != null
                    ? mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid()).getLatency()
                    : 0;
            if (sb.length() > 0) sb.append("  ");
            sb.append("Ping: ").append(p).append("ms");
        }
        return sb.toString();
    }
}