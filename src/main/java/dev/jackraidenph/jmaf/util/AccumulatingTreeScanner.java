package dev.jackraidenph.jmaf.util;

import com.sun.source.tree.*;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;

public class AccumulatingTreeScanner extends TreeScanner<Void, Void> {

    private final Predicate<TreeContext> treePredicate;
    private final List<TreeContext> toProcess = new LinkedList<>();

    private CompilationUnitTree currentUnit;
    private Symbol currentSymbol;
    private JCClassDecl currentClassDecl;

    public AccumulatingTreeScanner(Predicate<TreeContext> treePredicate) {
        this.treePredicate = treePredicate;
    }

    public List<TreeContext> getToProcess() {
        return toProcess;
    }

    @Override
    public Void visitCompilationUnit(CompilationUnitTree node, Void scanContext) {
        currentUnit = node;
        return super.visitCompilationUnit(node, scanContext);
    }


    @Override
    public Void visitClass(ClassTree node, Void scanContext) {
        if (node instanceof JCClassDecl classDecl) {
            currentSymbol = classDecl.sym;
            currentClassDecl = classDecl;
        }
        return super.visitClass(node, scanContext);
    }

    @Override
    public Void visitMethod(MethodTree node, Void scanContext) {
        if (node instanceof JCMethodDecl methodDecl) {
            currentSymbol = methodDecl.sym;
        }
        return super.visitMethod(node, scanContext);
    }

    @Override
    public Void visitVariable(VariableTree node, Void scanContext) {
        if (node instanceof JCVariableDecl variableDecl) {
            currentSymbol = variableDecl.sym;
        }

        return super.visitVariable(node, scanContext);
    }

    @Override
    public Void scan(Tree tree, Void scanContext) {
        TreeContext treeContext = new TreeContext(tree, currentClassDecl, currentSymbol, currentUnit);
        if (treePredicate.test(treeContext)) {
            toProcess.add(treeContext);
        }
        return super.scan(tree, scanContext);
    }

    public record TreeContext(Tree tree, JCClassDecl parent, Symbol symbol, CompilationUnitTree owner) {
        @Override
        public String toString() {
            Path path = Paths.get(owner.getSourceFile().toUri());
            return tree.getKind() + "@" + path.getFileName();
        }
    }
}
