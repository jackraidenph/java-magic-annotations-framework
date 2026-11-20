package annotation;

import dev.jackraidenph.jmaf.annotation.meta.Extends;
import dev.jackraidenph.jmaf.annotation.meta.OverrideAttribute;

@Extends(
        value = TestExtendedAnnotation.class,
        retainSelf = false
)
public @interface TestMarkerAnnotation {

    @OverrideAttribute(in = TestExtendedAnnotation.class, attribute = "bool")
    boolean implicitOverride() default true;

    @OverrideAttribute(in = TestExtendedAnnotation.class, attribute = "clazz")
    Class<?> explicitOverride();

}