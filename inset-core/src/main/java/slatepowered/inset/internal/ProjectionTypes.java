package slatepowered.inset.internal;

import slatepowered.inset.codec.DataCodec;
import slatepowered.inset.datastore.Datastore;
import slatepowered.inset.reflective.Key;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Support with projection-defining interfaces.
 */
public final class ProjectionTypes {

    private ProjectionTypes() { throw new UnsupportedOperationException("Utility class"); }

    /**
     * All compiled projection interfaces.
     */
    private static final Map<Class<?>, ProjectionInterface> compiledInterfaces = new ConcurrentHashMap<>();

    /**
     * Get or compile the projection data for the given interface.
     *
     * @param klass The interface class.
     * @return The interface.
     */
    public static ProjectionInterface compileProjectionInterface(Class<?> klass) {
        ProjectionInterface projectionInterface = compiledInterfaces.get(klass);
        if (projectionInterface != null) {
            return projectionInterface;
        }

        if (!klass.isInterface()) {
            return null;
        }

        Method[] methods = klass.getMethods();
        List<Method> fieldMethods = new ArrayList<>();
        Method keyMethod = null;
        for (Method method : methods) {
            // check for primary key method
            if (method.isAnnotationPresent(Key.class)) {
                keyMethod = method;
                continue;
            }

            fieldMethods.add(method);
        }

        compiledInterfaces.put(klass, projectionInterface = new ProjectionInterface(klass, keyMethod, fieldMethods));
        return projectionInterface;
    }

    /**
     * Get or compile a {@link ProjectionType} for the given class/type in the context
     * of the given datastore.
     *
     * @param klass The class.
     * @param datastore The datastore.
     * @return The {@link ProjectionType} instance.
     */
    @SuppressWarnings("unchecked")
    public static <K, T, V> ProjectionType getProjectionType(Class<V> klass, Datastore<K, T> datastore) {
        ProjectionType projectionType;

        // check for projection interface
        if (klass.isInterface()) {
            projectionType = compileProjectionInterface(klass);
        }

        // check for class
        else {
            projectionType = datastore.getCodecRegistry().getCodec(klass).expect(DataCodec.class);
        }

        if (projectionType != null)
            return projectionType;
        throw new UnsupportedOperationException("Unsupported type for projection of potentially partial data: " + klass);
    }

}
