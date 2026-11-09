package dev.jackraidenph.util;

import com.sun.source.tree.Tree;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskEvent.Kind;
import com.sun.source.util.TaskListener;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCModifiers;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
import dev.jackraidenph.annotation.TestGeneratedAnnotation;
import dev.jackraidenph.annotation.TestMarkerAnnotation;
import dev.jackraidenph.util.AccumulatingTreeScanner.ScanContext;
import dev.jackraidenph.util.AccumulatingTreeScanner.TreeContext;

public class JMAFTaskListener implements TaskListener {

    private final TreeMaker treeMaker;
    private final Names namesUtil;

    public JMAFTaskListener(Context context) {
        this.treeMaker = TreeMaker.instance(context);
        this.namesUtil = Names.instance(context);
    }

    @Override
    public void finished(TaskEvent e) {
        AccumulatingTreeScanner scanner = new AccumulatingTreeScanner(context -> {
            Tree tree = context.tree();

            return (tree instanceof JCModifiers modifiers)
                    && !modifiers.annotations.isEmpty()
                    && JavaInternalsUtil.treeContainsAnnotation(tree, context.owner(), TestMarkerAnnotation.class);
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

        //ADD IMPORT

        JCCompilationUnit compilationUnit = ((JCCompilationUnit) context.owner());

        List<JCTree> defs = compilationUnit.defs;

        Name packageName = namesUtil.fromString(TestGeneratedAnnotation.class.getPackageName());
        Name annotationName = namesUtil.fromString(TestGeneratedAnnotation.class.getSimpleName());

        compilationUnit.defs = JavaInternalsUtil.copyAndMutate(defs, l -> {
            JCFieldAccess fieldAccess = treeMaker.Select(treeMaker.Ident(packageName), annotationName);
            JCTree annotationImport = treeMaker.Import(fieldAccess, false);
            l.addFirst(annotationImport);
        });

        //ADD THE ANNOTATION ITSELF

        JCModifiers modifiers = (JCModifiers) context.tree();
        List<JCAnnotation> annotations = modifiers.annotations;
        modifiers.annotations = JavaInternalsUtil.copyAndMutate(annotations, l ->
                l.add(treeMaker.Annotation(treeMaker.Ident(annotationName), List.nil()))
        );
    }
}
