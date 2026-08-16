package com.krypton.client.module.setting;

public abstract class Setting<T> {
    public final String name;
    public final String description;
    protected T value;

    protected Setting(String name, String description, T value) {
        this.name = name;
        this.description = description;
        this.value = value;
    }

    public T get() {
        return value;
    }

    public void set(T value) {
        this.value = value;
    }

    public String getDisplayValue() {
        return String.valueOf(value);
    }
}