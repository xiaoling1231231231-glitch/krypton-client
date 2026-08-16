package com.krypton.client.module;

import com.krypton.client.module.setting.Setting;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.List;

public abstract class Module {
    public final String name;
    public final String description;
    public final Category category;
    private final List<Setting<?>> settings = new ArrayList<>();
    private boolean enabled;
    private int key = -1;

    protected static final MinecraftClient mc = MinecraftClient.getInstance();

    protected Module(String name, String description, Category category) {
        this.name = name;
        this.description = description;
        this.category = category;
    }

    protected Module(String name, Category category) {
        this(name, "", category);
    }

    protected void addSetting(Setting<?> setting) {
        settings.add(setting);
    }

    public List<Setting<?>> getSettings() {
        return settings;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return;
        this.enabled = enabled;
        if (enabled) onEnable(); else onDisable();
    }

    public void toggle() {
        setEnabled(!enabled);
    }

    public int getKey() {
        return key;
    }

    public void setKey(int key) {
        this.key = key;
    }

    protected void onEnable() {}
    protected void onDisable() {}

    // Called every client tick while enabled (and always, if override)
    public void onTick() {}
    public void onTickAlways() {}

    // Called when a setting changes
    public void onSettingChanged(Setting<?> setting) {}

    public String getInfo() {
        return null;
    }
}