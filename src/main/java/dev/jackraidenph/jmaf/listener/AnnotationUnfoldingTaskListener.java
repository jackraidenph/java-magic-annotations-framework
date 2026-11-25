package dev.jackraidenph.jmaf.listener;

import com.sun.source.tree.Tree;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskEvent.Kind;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAnnotatedType;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCModifiers;
import com.sun.tools.javac.util.Context;
import dev.jackraidenph.jmaf.annotation.meta.Extends;
import dev.jackraidenph.jmaf.api.AbstractContextTaskListener;
import dev.jackraidenph.jmaf.api.StringAttributeInterpolator;
import dev.jackraidenph.jmaf.graph.DirectedGraph;
import dev.jackraidenph.jmaf.graph.DirectedHashGraph;
import dev.jackraidenph.jmaf.util.AccumulatingTreeScanner;
import dev.jackraidenph.jmaf.util.AccumulatingTreeScanner.TreeContext;
import dev.jackraidenph.jmaf.util.TreesHelper;
import dev.jackraidenph.jmaf.util.TreesHelper.AnnotationWithAttributes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringJoiner;

public class AnnotationUnfoldingTaskListener extends AbstractContextTaskListener {
    public AnnotationUnfoldingTaskListener(Context context, Collection<StringAttributeInterpolator> interpolators) {
        super(context, interpolators);
    }

    private static <T> void checkCycles(DirectedHashGraph<T> graph, TreeContext context) {
        if (graph.hasCycles()) {
            StringJoiner joiner = new StringJoiner(" -> ");
            for (T o : graph.getLastEncounteredCycle()) {
                joiner.add(o.toString());
            }

            String loopStr = joiner.toString();
            throw new IllegalStateException(
                    "Encountered a loop while unfolding annotations on element [%s]: %s".formatted(context.symbol(), loopStr)
            );
        }
    }

    @Override
    public void started(TaskEvent e) {
        if (e.getCompilationUnit() == null || !e.getKind().equals(Kind.ANALYZE)) {
            return;
        }

        TreesHelper helper = new TreesHelper(getContext());

        AccumulatingTreeScanner scanner = new AccumulatingTreeScanner(context -> {
            Tree tree = context.tree();
            return TreesHelper.annotatedWithAny(tree) && helper.isAnyAnnotatedWith(tree, Extends.class) != null;
        });

        e.getCompilationUnit().accept(scanner, null);

        DirectedHashGraph<AnnotationWithAttributes> unfoldGraph = new DirectedHashGraph<>(true);

        //Repeat until everything is unfolded
        while (!scanner.getToProcess().isEmpty()) {
            for (TreeContext context : scanner.getToProcess()) {
                processTree(helper, context, unfoldGraph);
                checkCycles(unfoldGraph, context);
            }

            scanner.getToProcess().clear();
            e.getCompilationUnit().accept(scanner, null);
        }
    }

    private void processTree(TreesHelper helper, TreeContext context, DirectedGraph<AnnotationWithAttributes> graph) {
        JCCompilationUnit compilationUnit = ((JCCompilationUnit) context.owner());
        JCTree tree = (JCTree) context.tree();

        List<JCAnnotation> annotations = new ArrayList<>();

        com.sun.tools.javac.util.List<JCAnnotation> existing;
        if (tree instanceof JCModifiers modifiers) {
            existing = modifiers.annotations;
        } else if (tree instanceof JCAnnotatedType type) {
            existing = type.annotations;
        } else {
            throw new IllegalArgumentException("The tree type is not annotated");
        }

        for (JCAnnotation annotation : existing) {
            if (TreesHelper.needsUnfolding(annotation)) {
                List<JCAnnotation> jcAnnotations = helper.unfoldAnnotation(annotation, getInterpolators());
                annotations.addAll(jcAnnotations);

                for (JCAnnotation child : jcAnnotations) {
                    graph.addEdge(
                            AnnotationWithAttributes.fromJC(annotation),
                            AnnotationWithAttributes.fromJC(child)
                    );
                }
            } else {
                annotations.add(annotation);
                graph.addNode(AnnotationWithAttributes.fromJC(annotation));
            }
        }

        if (tree instanceof JCModifiers modifiers) {
            TreesHelper.clearAnnotations(modifiers);
        }

        helper.addAnnotationsRaw(tree, context.parent(), context.symbol(), annotations);
    }
}
