package slatepowered.inset.util;

import slatepowered.veru.misc.Throwables;

import java.lang.ref.WeakReference;
import java.lang.reflect.*;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Support for reflections.
 */
public final class Reflections {

    private Reflections() { throw new UnsupportedOperationException("Utility class"); }

    // Common methods
    public static final Method METHOD_OBJECT_EQUALS = getMethod(Object.class, "equals");
    public static final Method METHOD_OBJECT_TOSTRING = getMethod(Object.class, "toString");
    public static final Method METHOD_OBJECT_HASHCODE = getMethod(Object.class, "hashCode");

    private final static Map<String, WeakReference<Class<?>>> classCache = new WeakHashMap<>();

    public static Class<?> findClass(String name) {
        try {
            WeakReference<Class<?>> ref = classCache.get(name);
            if (ref != null) {
                return ref.get();
            }

            Class<?> klass = Class.forName(name);
            classCache.put(name, new WeakReference<>(klass));
            return klass;
        } catch (ClassNotFoundException ex) {
            return null;
        } catch (Throwable t) {
            Throwables.sneakyThrow(t);
            throw new AssertionError();
        }
    }

    public static Method getMethod(Class<?> klass, String name) {
        try {
            for (Method method : klass.getMethods()) {
                if (method.getName().equals(name)) {
                    return method;
                }
            }

            return null;
        } catch (Exception e) {
            Throwables.sneakyThrow(e);
            throw new AssertionError();
        }
    }

    public static Class<?> getClassForType(Type type) {
        if (type == null) {
            return null;
        } else if (type instanceof Class) {
            return (Class)type;
        } else if (type instanceof ParameterizedType) {
            return getClassForType(((ParameterizedType)type).getRawType());
        } else if (type instanceof AnnotatedType) {
            return getClassForType(((AnnotatedType)type).getType());
        } else if (type instanceof WildcardType) {
            return Object.class;
        } else if (type instanceof TypeVariable) {
            if (((TypeVariable)type).getBounds().length == 0) return Object.class;
            return getClassForType(((TypeVariable)type).getBounds()[0]);
        } else {
            throw new IllegalArgumentException("No support to get the base class from Type object of type: " + type.getClass().getSimpleName());
        }
    }

}
