package com.krypton.client.module.setting;

public class NumberSetting extends Setting<Double> {
    private final double min;
    private final double max;
    private final double step;

    public NumberSetting(String name, double value, double min, double max, double step) {
        super(name, "", value);
        this.min = min;
        this.max = max;
        this.step = step;
    }

    public double getMin() { return min; }
    public double getMax() { return max; }
    public double getStep() { return step; }

    public int getInt() {
        return (int) Math.round(value);
    }

    public float getFloat() {
        return value.floatValue();
    }

    public void setFloat(float v) {
        set((double) clamp(v));
    }

    public void setInt(int v) {
        set((double) clamp(v));
    }

    @Override
    public void set(Double v) {
        double clamped = clamp(v);
        if (step > 0) {
            clamped = Math.round(clamped / step) * step;
            clamped = Math.round(clamped * 1000.0) / 1000.0;
        }
        this.value = clamped;
    }

    private double clamp(double v) {
        return Math.max(min, Math.min(max, v));
    }

    @Override
    public String getDisplayValue() {
        if (step >= 1) return String.valueOf(getInt());
        return String.valueOf(value);
    }
}