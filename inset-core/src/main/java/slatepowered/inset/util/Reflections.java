package slatepowered.inset.util;

import slatepowered.veru.misc.Throwables;

import java.lang.reflect.Method;

/**
 * Support for reflections.
 */
public final class Reflections {

    // Common methods
    public static final Method METHOD_OBJECT_EQUALS = getMethod(Object.class, "equals");
    public static final Method METHOD_OBJECT_TOSTRING = getMethod(Object.class, "toString");
    public static final Method METHOD_OBJECT_HASHCODE = getMethod(Object.class, "hashCode");

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

}
