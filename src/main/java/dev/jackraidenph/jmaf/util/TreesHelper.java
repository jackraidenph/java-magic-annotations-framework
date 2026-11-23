package dev.jackraidenph.jmaf.util;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.PackageSymbol;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.comp.Annotate;
import com.sun.tools.javac.comp.AttrContext;
import com.sun.tools.javac.comp.Enter;
import com.sun.tools.javac.comp.Env;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
import dev.jackraidenph.jmaf.annotation.meta.Extends;
import dev.jackraidenph.jmaf.annotation.meta.OverrideAttribute;

import javax.lang.model.element.TypeElement;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TreesHelper {

    private final TreeMaker treeMaker;
    private final Names names;
    private final Context context;

    public TreesHelper(Context context) {
        this.context = context;
        this.treeMaker = TreeMaker.instance(context);
        this.names = Names.instance(context);
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

    public static boolean needsUnfolding(JCAnnotation annotation) {
        return annotation.annotationType.type.tsym.getAnnotation(Extends.class) != null;
    }

    public static void clearAnnotations(JCModifiers modifiers) {
        if (modifiers == null || modifiers.annotations.isEmpty()) {
            return;
        }

        modifiers.annotations = com.sun.tools.javac.util.List.from(new JCAnnotation[0]);
    }

    public TreeMaker wrapped() {
        return this.treeMaker;
    }

    public TreesHelper at(int pos) {
        treeMaker.at(pos);
        return this;
    }

    public Extends getAnyUnfoldingAnnotation(Tree tree) {
        com.sun.tools.javac.util.List<JCAnnotation> annotations = getAnnotations(tree);

        for (JCAnnotation annotation : annotations) {
            TypeSymbol annotationSymbol = annotation.annotationType.type.tsym;
            Extends a = annotationSymbol.getAnnotation(Extends.class);
            if (a != null) {
                return a;
            }
        }

        return null;
    }

    public Map<String, Class<? extends Function<?, ?>>> gatherTransformerTypes(TypeSymbol annotationSymbol) {
        return annotationSymbol.getEnclosedElements().stream()
                .filter(e -> e instanceof MethodSymbol)
                .map(e -> (MethodSymbol) e)
                .map(m -> m.getAnnotation(OverrideAttribute.class))
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        OverrideAttribute::attribute,
                        o -> Commons.getOrUnmirrorClass(o::transformer))
                );
    }

    public Map<String, Function<?, ?>> buildTransformers(Map<String, Class<? extends Function<?, ?>>> transformerTypes) {
        Map<String, Function<?, ?>> transformers = new HashMap<>();

        for (Entry<String, Class<? extends Function<?, ?>>> typeEntry : transformerTypes.entrySet()) {
            String attrName = typeEntry.getKey();
            Class<? extends Function<?, ?>> type = typeEntry.getValue();

            if (type == null || type.equals(OverrideAttribute.NoOp.class)) {
                continue;
            }

            try {
                Function<?, ?> transformer = type.getDeclaredConstructor().newInstance();
                transformers.put(attrName, transformer);
            } catch (NoSuchMethodException e) {
                throw new IllegalArgumentException("Failed to instantiate class [%s], maybe, it has not been compiled yet", e);
            } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        return transformers;
    }

    public void tryTransformAttributes(Map<String, Object> values, Map<String, ? extends Function<?, ?>> transformers) {
        for (Entry<String, Object> e : values.entrySet()) {
            String attributeName = e.getKey();
            //noinspection unchecked
            Function<Object, Object> transformer = (Function<Object, Object>) transformers.get(attributeName);
            if (transformer == null) {
                continue;
            }

            Object value = e.getValue();
            try {
                Object transformed = transformer.apply(value);
                values.put(attributeName, transformed);
            } catch (ClassCastException c) {
                throw new IllegalArgumentException("Transformer type [%s] argument type mismatch".formatted(transformer.getClass().getName()), c);
            }

        }
    }

    public List<JCAnnotation> unfoldTree(Tree tree) {
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
            if (needsUnfolding(annotation)) {
                annotations.addAll(unfoldAnnotation(annotation));
            } else {
                annotations.add(annotation);
            }
        }

        return annotations;
    }

    public List<JCAnnotation> unfoldAnnotation(JCAnnotation annotation) {
        TypeSymbol annotationSymbol;
        try {
            annotationSymbol = annotation.annotationType.type.tsym;
        } catch (NullPointerException e) {
            throw new IllegalStateException("Symtables not yet populated");
        }

        Extends a = annotationSymbol.getAnnotation(Extends.class);

        if (a == null) {
            return List.of();
        }

        Map<String, Class<? extends Function<?, ?>>> transformerTypesMap = gatherTransformerTypes(annotationSymbol);
        Map<String, Function<?, ?>> transformersMap = buildTransformers(transformerTypesMap);

        List<JCAnnotation> result = new ArrayList<>();
        for (AnnotationWithAttributes annot : unfoldRaw(annotation)) {
            Class<? extends Annotation> type = annot.type;
            Map<String, Object> attributes = annot.attributes;

            if (!transformersMap.isEmpty()) {
                tryTransformAttributes(attributes, transformersMap);
            }

            at(annotation.pos);
            result.add(makeAnnotation(annot));
        }

        if (a.retainSelf()) {
            result.add(annotation);
        }

        return result;
    }

    public List<AnnotationWithAttributes> unfoldRaw(JCAnnotation origin) {
        List<AnnotationWithAttributes> result = new ArrayList<>();

        Extends a = origin.annotationType.type.tsym.getAnnotation(Extends.class);

        if (a == null) {
            return List.of();
        }

        Class<? extends Annotation>[] array = Commons.getOrUnmirrorClassArray(a::value);
        for (Class<? extends Annotation> t : array) {
            AnnotationWithAttributes attr = tryResolveUnfolding(origin, t);
            if (attr != null) {
                result.add(attr);
            }
        }

        return result;
    }

    public AnnotationWithAttributes tryResolveUnfolding(JCAnnotation extensionOrigin, Class<? extends Annotation> extenstionTarget) {
        TypeSymbol tsym = extensionOrigin.annotationType.type.tsym;

        List<MethodSymbol> extensionAttributes = tsym.getEnclosedElements()
                .stream()
                .filter(s -> s instanceof MethodSymbol)
                .map(s -> (MethodSymbol) s)
                .filter(m -> {
                    OverrideAttribute overrideClause = m.getAnnotation(OverrideAttribute.class);
                    if (overrideClause == null) {
                        return false;
                    }

                    return Commons.getOrUnmirrorClass(overrideClause::in).equals(extenstionTarget);
                })
                .toList();

        if (extensionAttributes.isEmpty()) {
            return null;
        }

        Map<String, Object> attributes = new HashMap<>();

        Map<String, String> replacementMapping = new HashMap<>();
        //Process implicit attributes
        for (MethodSymbol attr : extensionAttributes) {
            OverrideAttribute overrideClause = attr.getAnnotation(OverrideAttribute.class);

            if (overrideClause == null) {
                continue;
            }

            replacementMapping.put(attr.name.toString(), overrideClause.attribute());

            Attribute attrVal = attr.getDefaultValue();
            if (attrVal == null) {
                continue;
            }
            Object val = attrVal.getValue();
            String key = overrideClause.attribute();

            attributes.put(key, val);
        }

        //Process explicit attributes
        for (JCExpression expression : extensionOrigin.args) {
            String key;
            Object val;
            if (expression instanceof JCAssign assign) {
                key = replacementMapping.get(assign.lhs.toString());
                if (key == null) {
                    continue;
                }
                val = assign.rhs;
            } else {
                key = names.value.toString();
                Object obj;
                if (expression instanceof JCLiteral literal) {
                    val = literal.value;
                } else {
                    val = expression;
                }
            }

            attributes.put(key, val);
        }

        return new AnnotationWithAttributes(extenstionTarget, attributes);
    }

    public void addAnnotationsRaw(Tree tree, JCClassDecl parent, Symbol symbol, Iterable<JCAnnotation> annotations) {
        ListBuffer<JCAnnotation> toAppend = new ListBuffer<>();
        for (JCAnnotation a : annotations) {
            toAppend.append(a);
        }

        com.sun.tools.javac.util.List<JCAnnotation> newAnnots;

        if (tree instanceof JCModifiers jcModifiers) {
            newAnnots = jcModifiers.annotations = jcModifiers.annotations.appendList(toAppend);
        } else if (tree instanceof JCAnnotatedType jcAnnotatedType) {
            newAnnots = jcAnnotatedType.annotations = jcAnnotatedType.annotations.appendList(toAppend);
        } else {
            //Other cases are covered by getAnnotations()
            throw new IllegalStateException();
        }

        Annotate annotate = Annotate.instance(context);
        Enter enter = Enter.instance(context);
        Env<AttrContext> env = enter.getEnv(parent.sym);
        annotate.annotateLater(newAnnots, env, symbol, parent);
    }

    public void removeAnnotation(Tree tree, JCClassDecl parent, Symbol symbol, String qualifiers) {
        com.sun.tools.javac.util.List<JCAnnotation> newAnnots;

        if (tree instanceof JCModifiers jcModifiers) {
            newAnnots = jcModifiers.annotations;
        } else if (tree instanceof JCAnnotatedType jcAnnotatedType) {
            newAnnots = jcAnnotatedType.annotations;
        } else {
            //Other cases are covered by getAnnotations()
            throw new IllegalStateException();
        }

        int i = 0;
        for (; i < newAnnots.size(); i++) {
            JCAnnotation a = newAnnots.get(i);
            if (a.type.tsym.getQualifiedName().contentEquals(qualifiers)) {
                break;
            }
        }

        if (i < newAnnots.size()) {
            newAnnots.remove(i);
        }

        Annotate annotate = Annotate.instance(context);
        Enter enter = Enter.instance(context);
        Env<AttrContext> env = enter.getEnv(parent.sym);
        annotate.annotateLater(newAnnots, env, symbol, parent);
    }

    public void addAnnotations(JCTree tree, JCClassDecl parent, Symbol symbol, Iterable<AnnotationWithAttributes> attributes) {
        com.sun.tools.javac.util.List<JCAnnotation> annotations = getAnnotations(tree);
        List<JCAnnotation> result = new ArrayList<>();
        for (AnnotationWithAttributes a : attributes) {
            at(tree.pos);
            result.add(makeAnnotation(a.type, a.attributes));
        }
        addAnnotationsRaw(tree, parent, symbol, result);
    }

    public Name fromString(String str) {
        return names.fromString(str);
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

    public JCAnnotation makeAnnotation(AnnotationWithAttributes attr) {
        return makeAnnotation(attr.type, attr.attributes);
    }

    public JCIdent makeSymboledIndent(Name name, ClassSymbol origin) {
        Symbol.MethodSymbol attrMethodSym = null;
        for (Symbol member : origin.members().getSymbolsByName(name)) {
            if (member instanceof Symbol.MethodSymbol) {
                attrMethodSym = (Symbol.MethodSymbol) member;
                break;
            }
        }

        return (attrMethodSym != null)
                ? treeMaker.Ident(attrMethodSym)
                : treeMaker.Ident(name);
    }

    public JCAnnotation makeAnnotation(Class<?> type, Map<String, Object> attributes) {
        TypeElement typeElement = JavacElements.instance(context).getTypeElement(type.getCanonicalName());
        JCExpression identity = treeMaker.QualIdent((Symbol) typeElement);

        List<JCExpression> attrs = new LinkedList<>();

        //Ensure key-value order if an insert-ordered map (Most commonly, Map.of()) is passed
        List<Entry<String, Object>> reverseEntryList = new ArrayList<>(attributes.entrySet());
        Collections.reverse(reverseEntryList);

        for (Entry<String, Object> a : reverseEntryList) {
            String key = a.getKey();
            Object value = a.getValue();

            Name name = fromString(key);

            JCAssign kv;
            //noinspection CastCanBeRemovedNarrowingVariableType
            JCIdent ident = makeSymboledIndent(name, (ClassSymbol) typeElement);
            if (value instanceof JCExpression e) {
                kv = treeMaker.Assign(ident, e);
            } else {
                JCLiteral lit = treeMaker.Literal(value);
                kv = treeMaker.Assign(ident, lit);
            }

            attrs.add(kv);
        }

        com.sun.tools.javac.util.List<JCExpression> attrsSun = com.sun.tools.javac.util.List.from(attrs);

        JCAnnotation jcAnnotation = treeMaker.Annotation(identity, attrsSun);
        jcAnnotation.type = JavacElements.instance(context).getTypeElement(type.getCanonicalName()).asType();

        return jcAnnotation;
    }

    public record AnnotationWithAttributes(Class<? extends Annotation> type, Map<String, Object> attributes) {
    }
}
