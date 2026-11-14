package dev.jackraidenph.jmaf;

import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.util.Context;
import dev.jackraidenph.jmaf.api.AbstractContextTaskListener;

import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.*;

public class JMAFCompilerPlugin implements Plugin {
    public static final String NAME = "JMAFCompilerPlugin";
    public static final String TASKS_FILE_PATH = "META-INF/jmaf.tasks";

    @Override
    public void init(JavacTask task, String... args) {
        Collection<AbstractContextTaskListener> listeners = constructListeners(
                loadListenerClasses(readListenerClasses()),
                ((BasicJavacTask) task).getContext()
        );

        for (AbstractContextTaskListener contextTaskListener : listeners) {
            task.addTaskListener(contextTaskListener);
        }
    }

    private Collection<String> readListenerClasses() {
        List<URL> listenerRegistryFiles = new ArrayList<>();
        getClass().getClassLoader().resources(TASKS_FILE_PATH).forEach(listenerRegistryFiles::add);

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

    private Collection<Class<? extends AbstractContextTaskListener>> loadListenerClasses(Iterable<String> classNames) {
        List<Class<? extends AbstractContextTaskListener>> classes = new LinkedList<>();
        for (String name : classNames) {
            try {
                Class<?> clazz = Class.forName(name);
                if (!AbstractContextTaskListener.class.isAssignableFrom(clazz)) {
                    throw new IllegalArgumentException("%s is not a subclass of %s".formatted(clazz.getName(), AbstractContextTaskListener.class.getName()));
                }
                classes.add(clazz.asSubclass(AbstractContextTaskListener.class));
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException(e);
            }
        }

        return classes;
    }

    private Collection<AbstractContextTaskListener> constructListeners(
            Iterable<Class<? extends AbstractContextTaskListener>> listenerClasses,
            Object... args
    ) {
        List<AbstractContextTaskListener> listeners = new LinkedList<>();
        for (Class<? extends AbstractContextTaskListener> clazz : listenerClasses) {
            try {
                AbstractContextTaskListener constructed = clazz
                        .getConstructor(Context.class)
                        .newInstance(args);

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