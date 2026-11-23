package dev.jackraidenph.jmaf;

import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.util.Context;
import dev.jackraidenph.jmaf.api.AbstractContextTaskListener;
import dev.jackraidenph.jmaf.api.StringAttributeInterpolator;

import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.*;

public class JMAFCompilerPlugin implements Plugin {
    public static final String NAME = "JMAFCompilerPlugin";
    public static final String LISTENERS_FILE_PATH = "META-INF/jmaf.listeners";
    public static final String INTERPOLATORS_FILE_PATH = "META-INF/jmaf.interpolators";

    @Override
    public void init(JavacTask task, String... args) {
        Collection<StringAttributeInterpolator> interpolators = constructClasses(
                loadClasses(readClasses(INTERPOLATORS_FILE_PATH), StringAttributeInterpolator.class),
                new Class[]{}
        );

        Collection<AbstractContextTaskListener> listeners = constructClasses(
                loadClasses(readClasses(LISTENERS_FILE_PATH), AbstractContextTaskListener.class),
                new Class[]{Context.class, Collection.class},
                ((BasicJavacTask) task).getContext(),
                interpolators
        );

        for (AbstractContextTaskListener contextTaskListener : listeners) {
            task.addTaskListener(contextTaskListener);
        }
    }

    private Collection<String> readClasses(String path) {
        List<URL> listenerRegistryFiles = new ArrayList<>();
        getClass().getClassLoader().resources(path).forEach(listenerRegistryFiles::add);

        Set<String> listenerClasses = new HashSet<>();
        for (URL url : listenerRegistryFiles) {
            try (InputStreamReader fis = new InputStreamReader(url.openStream())) {
                listenerClasses.addAll(fis.readAllLines());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return listenerClasses;
    }

    private <T> Collection<Class<? extends T>> loadClasses(Iterable<String> classNames, Class<? extends T> target) {
        List<Class<? extends T>> classes = new LinkedList<>();
        for (String name : classNames) {
            try {
                Class<?> clazz = Class.forName(name);
                if (!target.isAssignableFrom(clazz)) {
                    throw new IllegalArgumentException("%s is not a subclass of %s".formatted(clazz.getName(), target.getName()));
                }

                classes.add(clazz.asSubclass(target));
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException(e);
            }
        }

        return classes;
    }

    private <T> Collection<T> constructClasses(Iterable<Class<? extends T>> listenerClasses, Class<?>[] argTypes, Object... args) {
        List<T> listeners = new ArrayList<>();
        for (Class<? extends T> clazz : listenerClasses) {
            try {

                T constructed = clazz.getConstructor(argTypes).newInstance(args);
                listeners.add(constructed);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                     NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }

        return listeners;
    }

    @Override
    public String getName() {
        return NAME;
    }


    @Override
    public boolean autoStart() {
        return true;
    }
}