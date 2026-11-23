package util;

import dev.jackraidenph.jmaf.api.StringAttributeInterpolator;

public class TestInterpolator implements StringAttributeInterpolator {
    @Override
    public String id() {
        return "testInterpolator";
    }

    @Override
    public String apply(String key) {
        return "interpolated";
    }

    @Override
    public boolean filter(String key) {
        return key.equals("test");
    }
}
