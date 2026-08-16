package com.krypton.client.module;

public enum Category {
    COMBAT("Combat"),
    MOVEMENT("Movement"),
    RENDER("Render"),
    HUD("HUD"),
    MISC("Misc");

    private final String display;

    Category(String display) {
        this.display = display;
    }

    public String getDisplay() {
        return display;
    }
}