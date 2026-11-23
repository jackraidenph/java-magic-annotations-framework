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
import dev.jackraidenph.jmaf.api.AbstractContextTaskListener;
import dev.jackraidenph.jmaf.api.StringAttributeInterpolator;
import dev.jackraidenph.jmaf.util.AccumulatingTreeScanner;
import dev.jackraidenph.jmaf.util.AccumulatingTreeScanner.TreeContext;
import dev.jackraidenph.jmaf.util.TreesHelper;

import java.util.Collection;
import java.util.List;

public class AnnotationUnfoldingTaskListener extends AbstractContextTaskListener {
    public AnnotationUnfoldingTaskListener(Context context, Collection<StringAttributeInterpolator> interpolators) {
        super(context, interpolators);
    }

    @Override
    public void started(TaskEvent e) {
        TreesHelper helper = new TreesHelper(getContext());

        AccumulatingTreeScanner scanner = new AccumulatingTreeScanner(context -> {
            Tree tree = context.tree();

            return (
                    (tree instanceof JCModifiers modifiers && !modifiers.annotations.isEmpty())
                            || (tree instanceof JCAnnotatedType annotatedType && !annotatedType.annotations.isEmpty())
            ) && (helper.getAnyUnfoldingAnnotation(tree) != null);
        });

        if (e.getCompilationUnit() == null || !e.getKind().equals(Kind.ANALYZE)) {
            return;
        }

        e.getCompilationUnit().accept(scanner, null);

        //Repeat until everything is unfolded
        while (!scanner.getToProcess().isEmpty()) {
            for (TreeContext context : scanner.getToProcess()) {
                processTree(helper, context);
            }

            scanner.getToProcess().clear();
            e.getCompilationUnit().accept(scanner, null);
        }
    }

    private void processTree(TreesHelper helper, TreeContext context) {
        JCCompilationUnit compilationUnit = ((JCCompilationUnit) context.owner());
        JCTree tree = (JCTree) context.tree();

        List<JCAnnotation> annotationList = helper.unfoldTree(tree, this.getInterpolators());

        if (tree instanceof JCModifiers modifiers) {
            TreesHelper.clearAnnotations(modifiers);
        }

        helper.addAnnotationsRaw(tree, context.parent(), context.symbol(), annotationList);
    }
}
