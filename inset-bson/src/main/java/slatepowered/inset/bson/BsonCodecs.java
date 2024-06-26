package slatepowered.inset.bson;

import org.bson.Document;

import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.WeakHashMap;

public final class BsonCodecs {

    /**
     * The name to be given to the field which holds the class name present on
     * encoded abstract objects.
     */
    public static final String CLASS_NAME_FIELD = "__class";

    protected static final Map<Class<?>, Boolean> classesWithAbstractParents = new WeakHashMap<>();

    protected static boolean hasAbstractParentsOrIsAbstract(Class<?> klass) {
        if ((
                klass.isInterface() ||
                Modifier.isAbstract(klass.getModifiers()) ||
                klass.getInterfaces().length != 0
        ) && (
                // Special classes
                klass != Enum.class
        )) {
            return true;
        }

        Boolean result = classesWithAbstractParents.get(klass);
        if (result != null) {
            return result;
        }

        Class<?> superclass = klass.getSuperclass();
        return superclass != null && superclass != Object.class &&
                hasAbstractParentsOrIsAbstract(klass.getSuperclass());
    }

    // check if the class name of an object of the given
    // class should be written to the database
    protected static boolean shouldWriteClassName(Class<?> klass) {
        return !Modifier.isFinal(klass.getModifiers()) &&
                hasAbstractParentsOrIsAbstract(klass);
    }

    protected static boolean writeClassNameIfNeeded(Document document, Class<?> klass) {
        boolean b = shouldWriteClassName(klass);
        if (b) {
            document.put(BsonCodecs.CLASS_NAME_FIELD, klass.getName());
        }

        return b;
    }

}
