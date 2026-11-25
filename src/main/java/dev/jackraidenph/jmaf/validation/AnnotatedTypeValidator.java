package dev.jackraidenph.jmaf.validation;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.util.Context;
import dev.jackraidenph.jmaf.util.TreesHelper;

import java.util.List;

public class AnnotatedTypeValidator implements Validator {

    @Override
    public void validate(Tree annotatedElement, AnnotationTree annotation, CompilationUnitTree compilationUnit, Context context, List<String> args) {
        if (args == null || args.isEmpty()) {
            throw new IllegalArgumentException("%s takes in at least 1 argument, a type(s) an element must be assignable to".formatted(this.getClass().getSimpleName()));
        }

        if (!(annotatedElement instanceof JCTree jcTree) || !(compilationUnit instanceof JCCompilationUnit jcCompilationUnit)) {
            throw new IllegalStateException();
        }

        Type annotatedType = TreesHelper.getType(jcTree);

        boolean isAssignable = false;

        Types types = Types.instance(context);
        JavacElements elements = JavacElements.instance(context);

        for (String a : args) {
            ClassSymbol symbol = elements.getTypeElement(a);
            Type assignableTo = symbol.type;

            if (types.isAssignable(annotatedType, assignableTo)) {
                isAssignable = true;
                break;
            }
        }

        if (!isAssignable) {
            throw new AnnotationValidationException(this, jcTree.pos(), exceptionMessage(args, context), jcCompilationUnit.sourcefile, context);
        }
    }

    @Override
    public String exceptionMessage(List<String> args, Context context) {
        return "Element must be either of types: " + args;
    }
}
