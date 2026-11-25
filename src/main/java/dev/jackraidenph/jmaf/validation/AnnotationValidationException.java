package dev.jackraidenph.jmaf.validation;

import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
import dev.jackraidenph.jmaf.util.Commons;

import javax.tools.JavaFileObject;

public class AnnotationValidationException extends IllegalStateException {
    public AnnotationValidationException(Validator validator, DiagnosticPosition pos, String message, JavaFileObject source, Context context, Throwable cause) {
        super("Validation [%s] failed".formatted(validator.getClass().getSimpleName()), cause);
        Commons.Javac.log(pos, message, source, context);
    }

    public AnnotationValidationException(Validator validator, DiagnosticPosition pos, String message, JavaFileObject source, Context context) {
        this(validator, pos, message, source, context, null);
    }
}
