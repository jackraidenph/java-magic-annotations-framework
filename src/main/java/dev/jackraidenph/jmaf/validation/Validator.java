package dev.jackraidenph.jmaf.validation;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.util.Context;

import java.util.List;

public interface Validator {
    void validate(Tree annotatedElement, AnnotationTree annotation, CompilationUnitTree compilationUnit, Context context, List<String> args);

    String exceptionMessage(List<String> args, Context context);
}
