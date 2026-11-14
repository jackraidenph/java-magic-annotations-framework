package dev.jackraidenph.jmaf.util;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Consumer;

public class TreeMakerHelper {

    private final TreeMaker treeMaker;
    private final Names namesUtil;

    public TreeMakerHelper(Context context) {
        this.treeMaker = TreeMaker.instance(context);
        this.namesUtil = Names.instance(context);
    }

    //STATIC METHODS

    public static <T> com.sun.tools.javac.util.List<T> copyAndMutate(com.sun.tools.javac.util.List<T> list, Consumer<List<T>> mutator) {
        java.util.List<T> copy = new ArrayList<>(list);
        mutator.accept(copy);
        return com.sun.tools.javac.util.List.from(copy);
    }

    public static com.sun.tools.javac.util.List<JCAnnotation> getAnnotations(Tree tree) {
        com.sun.tools.javac.util.List<JCAnnotation> annotations;
        if (tree instanceof JCModifiers jcModifiers) {
            annotations = jcModifiers.annotations;
        } else if (tree instanceof JCAnnotatedType jcAnnotatedType) {
            annotations = jcAnnotatedType.annotations;
        } else {
            throw new IllegalArgumentException("Expected %s or %s, got %s".formatted(Kind.MODIFIERS, Kind.ANNOTATED_TYPE, tree.getKind()));
        }

        return annotations;
    }

    //PASS NULL AS OWNER IF LOOKING FOR ANNOTATIONS THAT DON'T REQUIRE IMPORTING (@Override, etc.)
    public static boolean treeContainsAnnotation(Tree tree, CompilationUnitTree owner, Class<? extends Annotation> type) {
        return treeContainsAnnotation(tree, owner, type.getPackageName(), type.getSimpleName());
    }

    public static boolean treeContainsAnnotation(Tree tree, Class<? extends Annotation> type) {
        return treeContainsAnnotation(tree, null, type);
    }

    public static boolean treeContainsAnnotation(Tree tree, String packageName, String name) {
        return treeContainsAnnotation(tree, null, packageName, name);
    }

    //PASS NULL AS OWNER IF LOOKING FOR ANNOTATIONS THAT DON'T REQUIRE IMPORTING (@Override, etc.)
    public static boolean treeContainsAnnotation(Tree tree, CompilationUnitTree owner, String packageName, String name) {
        com.sun.tools.javac.util.List<JCAnnotation> annotations = getAnnotations(tree);

        if (owner != null && !hasImport(owner, packageName, name)) {
            return false;
        }

        return annotations.stream()
                .map(JCAnnotation::getAnnotationType)
                .map(Objects::toString)
                .anyMatch(q -> q.equals(name));
    }

    public static boolean hasImport(CompilationUnitTree source, Class<?> clazz) {
        return hasImport(source, clazz.getPackageName(), clazz.getSimpleName());
    }

    public static boolean hasImport(CompilationUnitTree source, String packageName, String className) {
        return source.getImports()
                .stream()
                .map(ImportTree::getQualifiedIdentifier)
                .map(Tree::toString)
                .anyMatch(s -> {
                    String qualified = packageName + "." + className;
                    boolean startsWithOnDemand = s.endsWith("*") && qualified.startsWith(s.substring(0, s.length() - 1));
                    boolean isTheImport = qualified.equals(s);
                    return isTheImport || startsWithOnDemand;
                });
    }

    private static int findImportsIndex(JCCompilationUnit source) {
        int i = 0;
        while (!(source.defs.get(i) instanceof ImportTree)) {
            i++;
        }
        return i;
    }

    //TREE MAKER METHODS

    public TreeMaker wrapped() {
        return this.treeMaker;
    }

    public TreeMakerHelper at(int pos) {
        treeMaker.at(pos);
        return this;
    }

    public void addAnnotationAndImport(JCCompilationUnit source, JCTree tree, Class<?> clazz, Map<String, Object> attributes) {
        at(source.pos).addImportIfNotPresent(source, clazz);
        at(tree.pos).addAnnotation(tree, clazz, attributes);
    }

    public void addImportIfNotPresent(JCCompilationUnit source, Class<?> clazz) {
        if (!hasImport(source, clazz)) {
            addImport(source, clazz);
        }
    }

    public void addImport(JCCompilationUnit source, Class<?> clazz) {
        com.sun.tools.javac.util.List<JCTree> defs = source.defs;

        Name packageName = namesUtil.fromString(clazz.getPackageName());
        Name annotationName = namesUtil.fromString(clazz.getSimpleName());

        source.defs = TreeMakerHelper.copyAndMutate(defs, l -> {
            JCFieldAccess fieldAccess = treeMaker.Select(treeMaker.Ident(packageName), annotationName);
            JCImport annotationImport = treeMaker.Import(fieldAccess, false);
            l.add(findImportsIndex(source), annotationImport);
        });
    }

    public void addAnnotation(Tree tree, Class<?> type, Map<String, Object> attributes) {
        com.sun.tools.javac.util.List<JCAnnotation> annotations = getAnnotations(tree);
        JCAnnotation annotation = makeAnnotation(type, attributes);
        com.sun.tools.javac.util.List<JCAnnotation> mutated = copyAndMutate(annotations, l -> l.add(annotation));

        if (tree instanceof JCModifiers jcModifiers) {
            jcModifiers.annotations = mutated;
        } else if (tree instanceof JCAnnotatedType jcAnnotatedType) {
            jcAnnotatedType.annotations = mutated;
        }
        //Other cases are covered by getAnnotations()
    }

    public JCAnnotation makeAnnotation(Class<?> type, Map<String, Object> attributes) {
        Name annotName = namesUtil.fromString(type.getSimpleName());
        JCIdent identity = treeMaker.Ident(annotName);

        List<JCExpression> attrs = new LinkedList<>();

        //Ensure key-value order if an insert-ordered map (Most commonly, Map.of()) is passes
        List<Entry<String, Object>> reverseEntryList = new ArrayList<>(attributes.entrySet());
        Collections.reverse(reverseEntryList);

        for (Entry<String, Object> a : reverseEntryList) {
            String key = a.getKey();
            Object value = a.getValue();

            Name name = namesUtil.fromString(key);
            JCIdent ident = treeMaker.Ident(name);
            JCLiteral lit = treeMaker.Literal(value);

            JCAssign kv = treeMaker.Assign(ident, lit);

            attrs.add(kv);
        }

        com.sun.tools.javac.util.List<JCExpression> attrsSun = com.sun.tools.javac.util.List.from(attrs);

        return treeMaker.Annotation(identity, attrsSun);
    }
}
