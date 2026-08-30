package com.aethelon.module;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public record BoolSetting(String label, BooleanSupplier getter, Consumer<Boolean> setter)
        implements Setting {
    @Override
    public String valueText() {
        return getter.getAsBoolean() ? "ON" : "OFF";
    }

    @Override
    public void applyDelta(int delta) {
        setter.accept(!getter.getAsBoolean());
    }
}