package dev.jackraidenph.jmaf.util;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreeScanner;
import dev.jackraidenph.jmaf.util.AccumulatingTreeScanner.ScanContext;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;

public class AccumulatingTreeScanner extends TreeScanner<Void, ScanContext> {

    private final Predicate<TreeContext> treePredicate;
    private final List<TreeContext> toProcess = new LinkedList<>();

    public AccumulatingTreeScanner(Predicate<TreeContext> treePredicate) {
        this.treePredicate = treePredicate;
    }

    public List<TreeContext> getToProcess() {
        return toProcess;
    }

    @Override
    public Void visitCompilationUnit(CompilationUnitTree node, ScanContext scanContext) {
        scanContext.currentUnit = node;
        return super.visitCompilationUnit(node, scanContext);
    }

    @Override
    public Void scan(Tree tree, ScanContext scanContext) {
        TreeContext treeContext = new TreeContext(tree, scanContext.currentUnit);
        if (treePredicate.test(treeContext)) {
            toProcess.add(treeContext);
        }
        return super.scan(tree, scanContext);
    }

    public static class ScanContext {
        private CompilationUnitTree currentUnit;
    }

    public record TreeContext(Tree tree, CompilationUnitTree owner) {
        @Override
        public String toString() {
            Path path = Paths.get(owner.getSourceFile().toUri());
            return tree.getKind() + "@" + path.getFileName();
        }
    }
}
