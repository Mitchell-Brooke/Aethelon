package com.aethelon.module;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

public record IntSetting(String label, int min, int max, IntSupplier getter, IntConsumer setter) {
    public int value() {
        return getter.getAsInt();
    }

    public void set(int value) {
        setter.accept(value);
    }
}