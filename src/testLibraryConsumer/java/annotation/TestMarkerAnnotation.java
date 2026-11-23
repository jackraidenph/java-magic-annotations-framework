package annotation;

import dev.jackraidenph.jmaf.annotation.meta.Extends;
import dev.jackraidenph.jmaf.annotation.meta.OverrideAttribute;

import java.util.function.Function;

@Extends(
        value = TestExtendedAnnotation.class,
        retainSelf = false
)
public @interface TestMarkerAnnotation {

    @OverrideAttribute(in = TestExtendedAnnotation.class, attribute = "bool", transformer = TestTransformer.class)
    String implicitOverride() default "test";

    @OverrideAttribute(in = TestExtendedAnnotation.class, attribute = "clazz")
    Class<?> explicitOverride();

    class TestTransformer implements Function<String, Boolean> {
        @Override
        public Boolean apply(String s) {
            return s.equals(s.toLowerCase());
        }
    }
}