package slatepowered.inset.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Helpers/utilities for (query) values.
 */
public class ValueUtils {

    /**
     * Convert all known types of collection-like object to a set.
     *
     * @param value The value.
     * @return The set.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
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

}
