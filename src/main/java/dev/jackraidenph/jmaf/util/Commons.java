package dev.jackraidenph.jmaf.util;

import com.sun.tools.javac.code.Symbol.TypeSymbol;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
import com.sun.tools.javac.util.Log;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.*;
import javax.tools.JavaFileObject;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.function.Supplier;

public final class Commons {
    private Commons() {
    }

    private static String className(String flatName) {
        String topLevel = flatName.substring(flatName.lastIndexOf('.') + 1);
        int dollar = topLevel.lastIndexOf('$');
        if (dollar >= 0) {
            return topLevel.substring(dollar + 1);
        }

        return topLevel;
    }

    public static TypeElement mirrorToElement(TypeMirror typeMirror) {
        return (TypeElement) ((DeclaredType) typeMirror).asElement();
    }

    public static String descriptor(String primitiveName) {
        return switch (primitiveName) {
            case "boolean" -> "Z";
            case "byte" -> "B";
            case "short" -> "S";
            case "int" -> "I";
            case "long" -> "J";
            case "float" -> "F";
            case "double" -> "D";
            case "char" -> "C";
            default -> throw new UnsupportedOperationException();
        };
    }

    public static Class<?> fromTypeMirror(TypeMirror typeMirror) {
        String binaryName = Javac.binaryName(typeMirror);
        Class<?> clazz;
        try {
            clazz = Class.forName(binaryName);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("""
                    Failed to find class [%s] by the corresponding TypeMirror, \
                    most probably, the class is not loaded
                    """.formatted(binaryName)
            );
        }
        return clazz;
    }

    public static <T> Class<T> getOrUnmirrorClass(Supplier<Class<T>> fromAnnotationGetter) {
        try {
            return fromAnnotationGetter.get();
        } catch (MirroredTypeException e) {
            TypeMirror typeMirror = e.getTypeMirror();
            //noinspection unchecked
            return (Class<T>) fromTypeMirror(typeMirror);
        }
    }

    public static Class<? extends Annotation>[] getOrUnmirrorClassArray(Supplier<Class<? extends Annotation>[]> fromAnnotationGetter) {
        Class<? extends Annotation>[] classes;
        try {
            classes = fromAnnotationGetter.get();
            return classes;
        } catch (MirroredTypesException e) {
            List<? extends TypeMirror> typeMirrors = e.getTypeMirrors();
            //noinspection unchecked
            classes = new Class[typeMirrors.size()];

            int i = 0;
            for (TypeMirror typeMirror : typeMirrors) {
                String binaryName = Javac.binaryName(typeMirror);
                Class<? extends Annotation> clazz;
                try {
                    clazz = Class.forName(binaryName).asSubclass(Annotation.class);
                } catch (ClassNotFoundException ex) {
                    throw new IllegalArgumentException("""
                            Failed to find class [%s] by the corresponding TypeMirror, \
                            most probably, the class is not loaded
                            """.formatted(binaryName)
                    );
                }
                classes[i++] = clazz;
            }
            return classes;
        }
    }

    public static class Javac {

        public static void log(DiagnosticPosition pos, String message, JavaFileObject source, Context context) {
            Log log = Log.instance(context);

            JavaFileObject prevSource = log.useSource(source);
            try {
                log.error(pos.getPreferredPosition(), "proc.messager", message);
            } finally {
                log.useSource(prevSource);
            }
        }

        public static String binaryName(TypeMirror typeMirror) {
            if (typeMirror instanceof PrimitiveType primitiveType) {
                return primitiveType.toString();
            }

            if (typeMirror instanceof ArrayType arrayType) {
                TypeMirror component = arrayType.getComponentType();
                if (component instanceof PrimitiveType primitiveType) {
                    return "[" + descriptor(primitiveType.toString());
                } else {
                    return "[L" + binaryName(component) + ";";
                }
            }

            if (typeMirror instanceof DeclaredType declaredType) {
                return binaryName(mirrorToElement(declaredType));
            }

            throw new UnsupportedOperationException("Can't get [%s]'s binary name".formatted(typeMirror));
        }

        public static String binaryName(TypeElement element) {
            return ((TypeSymbol) element).flatName().toString();
        }
    }
}
