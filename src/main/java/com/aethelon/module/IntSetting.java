package com.aethelon.module;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

public record IntSetting(String label, int min, int max, IntSupplier getter, IntConsumer setter)
        implements Setting {
    public int value() {
        return getter.getAsInt();
    }

    public void set(int value) {
        setter.accept(value);
    }

    @Override
    public String valueText() {
        return String.valueOf(value());
    }

    @Override
    public void applyDelta(int delta) {
        set(Math.max(min, Math.min(max, value() + delta)));
    }
}