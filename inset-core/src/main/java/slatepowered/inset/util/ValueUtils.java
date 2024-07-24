package slatepowered.inset.util;

import slatepowered.veru.misc.ANSI;

import java.util.*;

/**
 * Helpers/utilities for (query) values.
 */
public final class ValueUtils {

    private ValueUtils() { throw new UnsupportedOperationException("Utility class"); }

    /**
     * Convert all known types of collection-like object to a set.
     *
     * @param value The value.
     * @return The set.
     */
    @SuppressWarnings({ "unchecked" })
    public static Set<Object> ensureSet(Object value) {
        if (value == null) {
            throw new IllegalArgumentException("Expected a collection-like value, got null");
        }

        if (value instanceof Set) {
            return (Set<Object>) value;
        }

        Class<?> valueClass = value.getClass();
        if (valueClass.isArray()) {
            Object[] array = (Object[]) value;
            final int len = array.length;
            Set<Object> set = new HashSet<>();
            for (int i = 0; i < len; i++)
                set.add(array[i]);
            return set;
        }

        if (value instanceof Collection) {
            return new HashSet<>((Collection<?>) value);
        }

        throw new IllegalArgumentException("Expected a collection-like value, got " + valueClass.getName());
    }

    /**
     * Cast the given boxed primitive value to a boxed version of the given target type.
     *
     * @param boxedValue The boxed value.
     * @param targetPrimitiveType The target primitive type.
     * @return The boxed primitive value.
     */
    public static Object castBoxedPrimitive(Object boxedValue, Class<?> targetPrimitiveType) {
        if (boxedValue == null) return null;

        if (targetPrimitiveType == int.class) return ((Number)boxedValue).intValue();
        if (targetPrimitiveType == long.class) return ((Number)boxedValue).longValue();
        if (targetPrimitiveType == double.class) return ((Number)boxedValue).doubleValue();
        if (targetPrimitiveType == float.class) return ((Number)boxedValue).floatValue();
        if (targetPrimitiveType == short.class) return ((Number)boxedValue).shortValue();
        if (targetPrimitiveType == byte.class) return ((Number)boxedValue).byteValue();

        return boxedValue;
    }

    /**
     * Cast the given source number of any type to the given target type.
     *
     * @param source The source number.
     * @param targetType The target type.
     * @return The result number.
     */
    public static Number castBoxedNumber(Number source, Class<?> targetType) {
        if (source == null) return null;
        if (source.getClass() == targetType) return source;
        if (targetType == Number.class) return source;

        if (targetType == Integer.class || targetType == int.class) return source.intValue();
        if (targetType == Long.class || targetType == long.class) return source.longValue();
        if (targetType == Double.class || targetType == double.class) return source.doubleValue();
        if (targetType == Float.class || targetType == float.class) return source.floatValue();
        if (targetType == Short.class || targetType == short.class) return source.shortValue();
        if (targetType == Byte.class || targetType == byte.class) return source.byteValue();

        throw new IllegalArgumentException("Unsupported target Number type: " + targetType);
    }

    private static String limitString(String str, int length) {
        if (str.length() > length) {
            str = str.substring(0, length - 3) + "...";
        }

        return str;
    }

    /**
     * Get a pretty, formatted and compact representation of the given value.
     */
    public static String prettyCompact(Object value) {
        if (value == null) { return ANSI.RED + "null" + ANSI.RESET; }
        if (value instanceof CharSequence) { return ANSI.CYAN + "\"" + value + "\"" + ANSI.RESET; }
        if (value instanceof Number) { return ANSI.BLUE + value + ANSI.RESET; }
        if (value instanceof Class) { return ANSI.PURPLE + value + ANSI.RESET; }
        if (value instanceof List) { return ANSI.GREEN + "List[" + ((List<?>) value).size() + "]" + ANSI.RESET; }
        if (value instanceof Map) { return ANSI.DARK_GREEN + "Map[" + ((Map<?, ?>) value).size() + "]" + ANSI.RESET; }

        return ANSI.YELLOW + limitString(value.toString(), 120) + ANSI.RESET;
    }

}
