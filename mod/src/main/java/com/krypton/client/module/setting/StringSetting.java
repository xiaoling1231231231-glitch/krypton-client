package com.krypton.client.module.setting;

public class StringSetting extends Setting<String> {
    private final int maxLength;

    public StringSetting(String name, String value) {
        this(name, value, 64);
    }

    public StringSetting(String name, String value, int maxLength) {
        super(name, "", value);
        this.maxLength = maxLength;
    }

    public void append(char c) {
        if (value.length() < maxLength) value += c;
    }

    public void backspace() {
        if (!value.isEmpty()) value = value.substring(0, value.length() - 1);
    }

    public void setText(String text) {
        if (text.length() > maxLength) text = text.substring(0, maxLength);
        this.value = text;
    }
}