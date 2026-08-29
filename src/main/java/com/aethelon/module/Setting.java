package com.aethelon.module;

public interface Setting {
    String label();

    String valueText();

    void applyDelta(int delta);
}