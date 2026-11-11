package pkg;

import dev.jackraidenph.annotation.TestMarkerAnnotation;
import org.junit.jupiter.api.Test;

public class TestClass {

    @Test
    public void testMethodNoArgs() {

    }

    @TestMarkerAnnotation
    public void testMethodArgs(int arg) {

    }

    public int testMethodArgsReturn(int arg) {
        return arg;
    }
}