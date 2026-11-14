package pkg;

import annotation.TestMarkerAnnotation;
import org.junit.jupiter.api.Test;

public class TestClass {

    @Test
    public void testMethodNoArgs() {
        System.out.println(getClass().getClassLoader());
    }

    @TestMarkerAnnotation
    public void testMethodArgs(int arg) {

    }

    public int testMethodArgsReturn(int arg) {
        return arg;
    }
}