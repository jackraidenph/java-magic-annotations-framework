package pkg;

import annotation.TestMarkerAnnotation;
import annotation.TestValidated;
import org.junit.jupiter.api.Test;
import util.TestExtensionTarget;

@TestMarkerAnnotation(explicitOverride = TestClass.class)
public class TestClass {

    @TestMarkerAnnotation(explicitOverride = TestClass.class)
    private final int TEST = 0;

    @TestValidated
    private final TestExtensionTarget TEST1 = null;

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