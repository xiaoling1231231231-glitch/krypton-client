package com.krypton.client.module.setting;

public class BooleanSetting extends Setting<Boolean> {
    public BooleanSetting(String name, boolean value) {
        super(name, "", value);
    }

    public BooleanSetting(String name, String description, boolean value) {
        super(name, description, value);
    }

    public void toggle() {
        value = !value;
    }
}