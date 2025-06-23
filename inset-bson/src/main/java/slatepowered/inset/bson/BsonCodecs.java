package slatepowered.inset.bson;

import org.bson.Document;
import org.bson.conversions.Bson;
import slatepowered.inset.codec.ClassDistinctionOverride;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

@ClassDistinctionOverride("")
public final class BsonCodecs {

    static final ClassDistinctionOverride NONE_OVERRIDE_INSTANCE;

    static {
        try {
            NONE_OVERRIDE_INSTANCE = BsonCodecs.class.getAnnotation(ClassDistinctionOverride.class);
        } catch (Exception ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    /**
     * The name to be given to the field which holds the class name present on
     * encoded abstract objects.
     */
    public static final String CLASS_NAME_FIELD = "__class";

    static final Map<Class<?>, Boolean> classesWithAbstractParents = new WeakHashMap<>();
    static final Map<Class<?>, ClassDistinctionOverride> distinctionOverrideClasses = new HashMap<>();

    static ClassDistinctionOverride getClassDistinctionOverride(Class<?> expectedType) {
        return expectedType.getAnnotation(ClassDistinctionOverride.class);
    }

    static boolean hasAbstractParentsOrIsAbstract(Class<?> klass) {
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
    static boolean shouldWriteClassName(Class<?> klass) {
        return !Modifier.isFinal(klass.getModifiers()) &&
                hasAbstractParentsOrIsAbstract(klass);
    }

    static boolean writeClassNameIfNeeded(Document document, Class<?> klass) {
        boolean b = shouldWriteClassName(klass);
        if (b) {
            document.put(BsonCodecs.CLASS_NAME_FIELD, klass.getName());
        }

        return b;
    }

}
