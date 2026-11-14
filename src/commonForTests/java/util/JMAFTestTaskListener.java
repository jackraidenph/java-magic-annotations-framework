package util;

import com.sun.source.tree.Tree;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskEvent.Kind;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCModifiers;
import com.sun.tools.javac.util.Context;
import annotation.TestGeneratedAnnotation;
import annotation.TestMarkerAnnotation;
import dev.jackraidenph.AbstractContextTaskListener;
import dev.jackraidenph.util.AccumulatingTreeScanner;
import dev.jackraidenph.util.AccumulatingTreeScanner.ScanContext;
import dev.jackraidenph.util.AccumulatingTreeScanner.TreeContext;
import dev.jackraidenph.util.TreeMakerHelper;

import java.util.Map;

public class JMAFTestTaskListener extends AbstractContextTaskListener {

    private final TreeMakerHelper treeMakerHelper;

    public JMAFTestTaskListener(Context context) {
        super(context);
        this.treeMakerHelper = new TreeMakerHelper(context);
    }

    @Override
    public void finished(TaskEvent e) {
        AccumulatingTreeScanner scanner = new AccumulatingTreeScanner(context -> {
            Tree tree = context.tree();

            return (tree instanceof JCModifiers modifiers)
                    && !modifiers.annotations.isEmpty()
                    && TreeMakerHelper.treeContainsAnnotation(tree, context.owner(), TestMarkerAnnotation.class);
        });

        if (e.getCompilationUnit() == null || !e.getKind().equals(Kind.PARSE)) {
            return;
        }

        e.getCompilationUnit().accept(scanner, new ScanContext());

        for (TreeContext context : scanner.getToProcess()) {
            processTree(context);
        }
    }

    private void processTree(TreeContext context) {
        JCCompilationUnit compilationUnit = ((JCCompilationUnit) context.owner());
        JCTree tree = (JCTree) context.tree();

        treeMakerHelper.addAnnotationAndImport(
                compilationUnit,
                tree,
                TestGeneratedAnnotation.class,
                Map.of("value", "test", "number", 2)
        );
    }
}
