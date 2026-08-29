package com.aethelon.module;

import java.util.function.Consumer;
import java.util.function.Supplier;

public record ChoiceSetting(String label, Option[] options, Supplier<String> getter, Consumer<String> setter)
        implements Setting {

    public record Option(String value, String display) {
    }

    @Override
    public String valueText() {
        String current = getter.get();
        for (Option option : options) {
            if (option.value().equals(current)) {
                return option.display();
            }
        }
        return current;
    }

    @Override
    public void applyDelta(int delta) {
        int index = indexOf(getter.get());
        int next = ((index + delta) % options.length + options.length) % options.length;
        setter.accept(options[next].value());
    }

    private int indexOf(String value) {
        for (int i = 0; i < options.length; i++) {
            if (options[i].value().equals(value)) {
                return i;
            }
        }
        return 0;
    }
}