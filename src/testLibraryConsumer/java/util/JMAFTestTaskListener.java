package util;

import annotation.TestGeneratedAnnotation;
import annotation.TestMarkerAnnotation;
import com.sun.source.tree.Tree;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskEvent.Kind;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCModifiers;
import com.sun.tools.javac.util.Context;
import dev.jackraidenph.jmaf.api.AbstractContextTaskListener;
import dev.jackraidenph.jmaf.util.AccumulatingTreeScanner;
import dev.jackraidenph.jmaf.util.AccumulatingTreeScanner.TreeContext;
import dev.jackraidenph.jmaf.util.TreesHelper;
import dev.jackraidenph.jmaf.util.TreesHelper.AnnotationWithAttributes;

import java.util.List;
import java.util.Map;

public class JMAFTestTaskListener extends AbstractContextTaskListener {

    private final TreesHelper treeMakerHelper;

    public JMAFTestTaskListener(Context context) {
        super(context);
        this.treeMakerHelper = new TreesHelper(context);
    }

    @Override
    public void started(TaskEvent e) {
        if (e.getCompilationUnit() == null || !e.getKind().equals(Kind.ANALYZE)) {
            return;
        }

        AccumulatingTreeScanner scanner = new AccumulatingTreeScanner(context -> {
            Tree tree = context.tree();
            if (tree == null) {
                return false;
            }
            return (tree instanceof JCModifiers modifiers)
                    && !modifiers.annotations.isEmpty()
                    && TreesHelper.treeContainsAnnotation(tree, context.owner(), TestMarkerAnnotation.class);
        });

        e.getCompilationUnit().accept(scanner, null);

        for (TreeContext context : scanner.getToProcess()) {
            processTree(context);
        }
    }

    private void processTree(TreeContext context) {
        JCCompilationUnit compilationUnit = ((JCCompilationUnit) context.owner());
        JCTree tree = (JCTree) context.tree();

        treeMakerHelper.addAnnotations(
                tree,
                context.parent(),
                context.symbol(),
                List.of(
                        new AnnotationWithAttributes(
                                TestGeneratedAnnotation.class,
                                Map.of("value", "test", "number", 2)
                        )
                )
        );
    }
}
