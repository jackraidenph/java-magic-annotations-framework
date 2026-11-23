package pkg;

import annotation.TestMarkerAnnotation;
import org.junit.jupiter.api.Test;

@TestMarkerAnnotation(explicitOverride = TestClass.class)
public class TestClass {

    @TestMarkerAnnotation(explicitOverride = TestClass.class)
    private final int TEST = 0;

    @Test
    public void testMethodNoArgs() {
        System.out.println(getClass().getClassLoader());
    }

    @TestMarkerAnnotation(explicitOverride = TestClass.class)
    public void testMethodArgs(@TestMarkerAnnotation(explicitOverride = TestClass.class, interpolated = true) int arg) {

    }

    public int testMethodArgsReturn(int arg) {
        return arg;
    }
}