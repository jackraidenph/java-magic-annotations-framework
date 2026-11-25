package annotation;

import dev.jackraidenph.jmaf.annotation.validation.Validated;
import dev.jackraidenph.jmaf.validation.AnnotatedTypeValidator;

@Validated(value = AnnotatedTypeValidator.class, args = "util.TestExtensionTarget")
public @interface TestValidated {
}
