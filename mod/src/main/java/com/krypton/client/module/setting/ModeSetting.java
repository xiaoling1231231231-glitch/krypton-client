package com.krypton.client.module.setting;

public class ModeSetting extends Setting<String> {
    private final String[] modes;

    public ModeSetting(String name, String value, String... modes) {
        super(name, "", value);
        this.modes = modes;
    }

    public String[] getModes() {
        return modes;
    }

    public void cycle(boolean forward) {
        int idx = index();
        idx = (idx + (forward ? 1 : -1) + modes.length) % modes.length;
        value = modes[idx];
    }

    public int index() {
        for (int i = 0; i < modes.length; i++) {
            if (modes[i].equals(value)) return i;
        }
        return 0;
    }

    public boolean is(String mode) {
        return value.equals(mode);
    }

    @Override
    public void set(String value) {
        for (String m : modes) {
            if (m.equals(value)) {
                this.value = m;
                return;
            }
        }
    }
}