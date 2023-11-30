package slatepowered.store.serializer;

import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Stores {@link DataSerializer}s by class.
 */
@RequiredArgsConstructor
public class SerializerRegistry {

    /** The factory which creates new serializers. */
    private final SerializerFactory factory;

    /** The data serializer cache. */
    private final Map<Class<?>, DataSerializer<?>> serializerMap = new HashMap<>();

    @SuppressWarnings("unchecked")
    public <T> DataSerializer<T> getOrCreate(Class<T> klass) {
        return (DataSerializer<T>) serializerMap.computeIfAbsent(klass, k -> factory.create(this, k));
    }

    @SuppressWarnings("unchecked")
    public <T, S extends DataSerializer<T>> DataSerializer<T> getOrCreate(Class<T> klass, BiFunction<SerializerRegistry, Class<T>, S> factory) {
        return (DataSerializer<T>) serializerMap.computeIfAbsent(klass, k -> factory.apply(this, klass));
    }

    public SerializerRegistry register(Class<?> klass, DataSerializer<?> serializer) {
        serializerMap.put(klass, serializer);
        return this;
    }

}
