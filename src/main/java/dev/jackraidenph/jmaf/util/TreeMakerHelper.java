package dev.jackraidenph.jmaf.util;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.PackageSymbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.comp.Annotate;
import com.sun.tools.javac.comp.AttrContext;
import com.sun.tools.javac.comp.Enter;
import com.sun.tools.javac.comp.Env;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.StreamSupport;

public class TreeMakerHelper {

    private final TreeMaker treeMaker;
    private final Names namesUtil;
    private final Context context;

    public TreeMakerHelper(Context context) {
        this.context = context;
        this.treeMaker = TreeMaker.instance(context);
        this.namesUtil = Names.instance(context);
    }

    //STATIC METHODS


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
                .filter(Objects::nonNull)
                .map(q -> (JCFieldAccess) q)
                .map(f -> f.selected + "." + f.name)
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

    private static String className(String flatName) {
        String topLevel = flatName.substring(flatName.lastIndexOf('.') + 1);
        int dollar = topLevel.lastIndexOf('$');
        if (dollar >= 0) {
            return topLevel.substring(dollar + 1);
        }

        return topLevel;
    }

    public TreeMaker wrapped() {
        return this.treeMaker;
    }

    public TreeMakerHelper at(int pos) {
        treeMaker.at(pos);
        return this;
    }

    public List<String> resolveNamesAgainstSymbols(Collection<String> names) {
        return StreamSupport.stream(
                        Symtab.instance(context).getAllClasses().spliterator(),
                        false
                )
                .map(ClassSymbol::flatName)
                .map(Objects::toString)
                .filter(s -> names.contains(className(s)))
                .toList();
    }

    public boolean treeContainsUnfoldingAnnotation(Tree tree) {
        com.sun.tools.javac.util.List<JCAnnotation> annotations = getAnnotations(tree);

        Set<String> annotationNames = new HashSet<>();
        for (JCAnnotation annotation : annotations) {
            annotationNames.add(annotation.getAnnotationType().toString());
        }

        List<String> annotationImports = resolveNamesAgainstSymbols(annotationNames);

        System.out.println(annotationImports);

        return true;
    }

//    public void addAnnotationAndImport(JCCompilationUnit source, JCTree tree, JCClassDecl parentSymbol, Symbol symbol, Class<?> clazz, Map<String, Object> attributes) {
//        at(source.pos).addImportIfNotPresent(source, clazz);
//        at(tree.pos).addAnnotation(tree, parentSymbol, symbol, clazz, attributes);
//    }
//
//    public void addImportIfNotPresent(JCCompilationUnit source, Class<?> clazz) {
//        if (!hasImport(source, clazz)) {
//            addImport(source, clazz);
//        }
//    }
//
//    public void addImport(JCCompilationUnit source, Class<?> clazz) {
//        com.sun.tools.javac.util.List<JCTree> defs = source.defs;
//
//        Name packageName = namesUtil.fromString(clazz.getPackageName());
//        Name annotationName = namesUtil.fromString(clazz.getSimpleName());
//
//        JCIdent pkg = treeMaker.Ident(packageName);
//        pkg.sym = packageFromString(clazz.getPackageName());
//
//        JCFieldAccess fieldAccess = treeMaker.Select(pkg, annotationName);
//        JCImport annotationImport = treeMaker.Import(fieldAccess, false);
//
//        source.defs = source.defs.append(annotationImport);
//    }

    public void addAnnotation(Tree tree, JCClassDecl parent, Symbol symbol, Class<?> type, Map<String, Object> attributes) {
        com.sun.tools.javac.util.List<JCAnnotation> annotations = getAnnotations(tree);
        JCAnnotation annotation = makeAnnotation(type, attributes);

        com.sun.tools.javac.util.List<JCAnnotation> newAnnots;

        if (tree instanceof JCModifiers jcModifiers) {
            newAnnots = jcModifiers.annotations = jcModifiers.annotations.append(annotation);
        } else if (tree instanceof JCAnnotatedType jcAnnotatedType) {
            newAnnots = jcAnnotatedType.annotations = jcAnnotatedType.annotations.append(annotation);
        } else {
            //Other cases are covered by getAnnotations()
            throw new IllegalStateException();
        }

        Annotate annotate = Annotate.instance(context);
        Enter enter = Enter.instance(context);
        Env<AttrContext> env = enter.getEnv(parent.sym);
        annotate.annotateLater(newAnnots, env, symbol, parent);
    }

    public Name fromString(String str) {
        return namesUtil.fromString(str);
    }

    public ClassSymbol classFromString(String str) {
        Iterator<ClassSymbol> iterator = Symtab.instance(context).getClassesForName(fromString(str)).iterator();
        if (!iterator.hasNext()) {
            return null;
        }
        return iterator.next();
    }

    public PackageSymbol packageFromString(String str) {
        Iterator<PackageSymbol> iterator = Symtab.instance(context).getPackagesForName(fromString(str)).iterator();
        if (!iterator.hasNext()) {
            return null;
        }
        return iterator.next();
    }

    public JCExpression makeQualIdent(String type) {
        Symbol symbol = classFromString(type);
        return treeMaker.QualIdent(symbol);
    }

    public JCExpression makeQualIdent(Class<?> type) {
        String[] parts = type.getCanonicalName().split("\\.");
        JCExpression expr = treeMaker.Ident(namesUtil.fromString(parts[0]));
        for (int i = 1; i < parts.length; i++) {
            expr = treeMaker.Select(expr, namesUtil.fromString(parts[i]));
        }
        return expr;
    }

    public JCAnnotation makeAnnotation(Class<?> type, Map<String, Object> attributes) {
        Symbol sym = classFromString(type.getName());
        JCExpression identity = makeQualIdent(type);

        List<JCExpression> attrs = new LinkedList<>();

        //Ensure key-value order if an insert-ordered map (Most commonly, Map.of()) is passes
        List<Entry<String, Object>> reverseEntryList = new ArrayList<>(attributes.entrySet());
        Collections.reverse(reverseEntryList);

        for (Entry<String, Object> a : reverseEntryList) {
            String key = a.getKey();
            Object value = a.getValue();

            Name name = fromString(key);
            JCIdent ident = treeMaker.Ident(name);

            JCLiteral lit = treeMaker.Literal(value);

            JCAssign kv = treeMaker.Assign(ident, lit);

            attrs.add(kv);
        }

        com.sun.tools.javac.util.List<JCExpression> attrsSun = com.sun.tools.javac.util.List.from(attrs);

        return treeMaker.Annotation(identity, attrsSun);
    }
}
