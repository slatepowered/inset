package slatepowered.store.serializer;

/**
 * Produces {@link DataSerializer}s at runtime.
 */
@FunctionalInterface
public interface SerializerFactory {

    /**
     * Create a new initial data serializer for the given class in
     * context of the provided registry.
     *
     * @param registry The registry.
     * @param klass The class to serialize.
     * @param <T> The serialized type.
     * @return The serializer.
     */
    <T> DataSerializer<T> create(SerializerRegistry registry, Class<T> klass);

}
