package dev.jackraidenph.jmaf.annotation.validation;

import dev.jackraidenph.jmaf.validation.Validator;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.ANNOTATION_TYPE)
public @interface Validated {
    Class<? extends Validator> value();

    String[] args() default {};
}
