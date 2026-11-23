package dev.jackraidenph.jmaf.api;

public interface StringAttributeInterpolator {
    String id();

    String apply(String key);

    default boolean filter(String key) {
        return true;
    }
}
