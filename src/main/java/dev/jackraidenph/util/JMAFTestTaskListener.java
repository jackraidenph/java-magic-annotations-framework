package dev.jackraidenph.util;

import com.sun.source.tree.Tree;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskEvent.Kind;
import com.sun.source.util.TaskListener;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCModifiers;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Names;
import dev.jackraidenph.annotation.TestGeneratedAnnotation;
import dev.jackraidenph.annotation.TestMarkerAnnotation;
import dev.jackraidenph.util.AccumulatingTreeScanner.ScanContext;
import dev.jackraidenph.util.AccumulatingTreeScanner.TreeContext;

import java.util.Map;

public class JMAFTestTaskListener implements TaskListener {

    private final TreeMakerHelper treeMakerHelper;
    private final Names namesUtil;

    public JMAFTestTaskListener(Context context) {
        this.treeMakerHelper = new TreeMakerHelper(context);
        this.namesUtil = Names.instance(context);
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
