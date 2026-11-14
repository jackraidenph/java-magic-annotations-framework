package dev.jackraidenph.jmaf.annotation.meta;

import java.lang.annotation.*;
import java.util.function.Function;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Replaces {
    String attribute();

    Class<? extends Annotation> in();

    Class<? extends Function<?, ?>> transformer() default NoOp.class;

    final class NoOp implements Function<Object, Object> {
        @Override
        public Object apply(Object object) {
            return object;
        }
    }
}
