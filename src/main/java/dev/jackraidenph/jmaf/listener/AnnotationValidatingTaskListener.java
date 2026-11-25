package dev.jackraidenph.jmaf.listener;

import com.sun.source.tree.AnnotatedTypeTree;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskEvent.Kind;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.util.Context;
import dev.jackraidenph.jmaf.annotation.validation.Validated;
import dev.jackraidenph.jmaf.api.AbstractContextTaskListener;
import dev.jackraidenph.jmaf.api.StringAttributeInterpolator;
import dev.jackraidenph.jmaf.util.AccumulatingTreeScanner;
import dev.jackraidenph.jmaf.util.AccumulatingTreeScanner.TreeContext;
import dev.jackraidenph.jmaf.util.Commons;
import dev.jackraidenph.jmaf.util.TreesHelper;
import dev.jackraidenph.jmaf.validation.Validator;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class AnnotationValidatingTaskListener extends AbstractContextTaskListener {
    public AnnotationValidatingTaskListener(Context context, Collection<StringAttributeInterpolator> interpolators) {
        super(context, interpolators);
    }

    @Override
    public void started(TaskEvent e) {
        if (e.getCompilationUnit() == null || !e.getKind().equals(Kind.ANALYZE)) {
            return;
        }

        TreesHelper helper = new TreesHelper(getContext());

        AccumulatingTreeScanner scanner = new AccumulatingTreeScanner(context -> {
            Tree tree = context.tree();

            if (tree instanceof AnnotatedTypeTree annotatedTypeTree) {
                return TreesHelper.annotatedWithAny(annotatedTypeTree) && helper.isAnyAnnotatedWith(annotatedTypeTree, Validated.class) != null;
            }

            ModifiersTree modifiersTree = TreesHelper.tryGetModifiers(tree);

            if (modifiersTree != null) {
                return TreesHelper.annotatedWithAny(modifiersTree) && helper.isAnyAnnotatedWith(modifiersTree, Validated.class) != null;
            }

            return false;
        });

        e.getCompilationUnit().accept(scanner, null);

        for (TreeContext treeContext : scanner.getToProcess()) {
            processTree(helper, treeContext, getContext());
        }
    }

    private void processTree(TreesHelper helper, TreeContext treeContext, Context context) {
        Tree tree = treeContext.tree();

        List<JCAnnotation> annotations;
        if (tree instanceof AnnotatedTypeTree annotatedTypeTree) {
            annotations = TreesHelper.getAnnotations(annotatedTypeTree);
        } else {
            ModifiersTree modifiersTree = TreesHelper.tryGetModifiers(tree);
            if (modifiersTree != null) {
                annotations = TreesHelper.getAnnotations(modifiersTree);
            } else {
                throw new IllegalStateException();
            }
        }

        Map<Tree, Collection<Validated>> validated = helper.getAllAnnotatedWith(annotations, Validated.class);

        for (Entry<Tree, Collection<Validated>> e : validated.entrySet()) {

            if (!(e.getKey() instanceof AnnotationTree annotationTree)) {
                throw new IllegalStateException();
            }

            for (Validated v : e.getValue()) {
                Class<? extends Validator> validatorType = Commons.getOrUnmirrorClass(v::value);
                List<String> args = List.of(v.args());

                try {
                    Constructor<? extends Validator> noArgConst = validatorType.getDeclaredConstructor();
                    noArgConst.setAccessible(true);
                    Validator validator = noArgConst.newInstance();

                    validator.validate(tree, annotationTree, treeContext.owner(), context, args);
                } catch (NoSuchMethodException | InvocationTargetException | InstantiationException |
                         IllegalAccessException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
    }
}
