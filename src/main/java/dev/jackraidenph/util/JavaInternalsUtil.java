package dev.jackraidenph.util;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.tree.JCTree.JCAnnotatedType;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCModifiers;
import com.sun.tools.javac.util.List;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Consumer;

public class JavaInternalsUtil {

    public static <T> List<T> copyAndMutate(List<T> list, Consumer<java.util.List<T>> mutator) {
        java.util.List<T> copy = new ArrayList<>(list);
        mutator.accept(copy);
        return List.from(copy);
    }

    public static boolean treeContainsAnnotation(Tree tree, CompilationUnitTree owner, Class<? extends Annotation> type) {
        return treeContainsAnnotation(tree, owner, type.getPackageName(), type.getSimpleName());
    }

    public static boolean treeContainsAnnotation(Tree tree, Class<? extends Annotation> type) {
        return treeContainsAnnotation(tree, null, type);
    }

    public static boolean treeContainsAnnotation(Tree tree, String packageName, String name) {
        return treeContainsAnnotation(tree, null, packageName, name);
    }

    public static boolean treeContainsAnnotation(Tree tree, CompilationUnitTree owner, String packageName, String name) {
        List<JCAnnotation> annotations;
        if (tree instanceof JCModifiers jcModifiers) {
            annotations = jcModifiers.annotations;
        } else if (tree instanceof JCAnnotatedType jcAnnotatedType) {
            annotations = jcAnnotatedType.annotations;
        } else {
            throw new IllegalArgumentException("Expected %s or %s, got %s".formatted(Kind.MODIFIERS, Kind.ANNOTATED_TYPE, tree.getKind()));
        }

        String qualifiedName = packageName + '.' + name;

        if (owner != null) {
            if (owner.getImports().stream()
                    .map(ImportTree::getQualifiedIdentifier)
                    .map(Objects::toString)
                    .noneMatch(i -> i.equals(qualifiedName))
            ) {
                return false;
            }
        }

        return annotations.stream()
                .map(JCAnnotation::getAnnotationType)
                .map(Objects::toString)
                .anyMatch(q -> q.equals(name));
    }
}
