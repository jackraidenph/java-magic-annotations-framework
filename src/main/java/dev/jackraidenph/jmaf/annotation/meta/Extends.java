package dev.jackraidenph.jmaf.annotation.meta;

import java.lang.annotation.*;

@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Extends {
    Class<? extends Annotation>[] value();

    /**
     *
     * @return Whether the annotation stays after unfloding, defaults to false, since it may cause recursive behaviour
     */
    boolean retainSelf() default false;
}
