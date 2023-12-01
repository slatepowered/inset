package slatepowered.inset.codec;

import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * Stores {@link ValueCodec}s by class.
 */
@RequiredArgsConstructor
public class CodecRegistry {

    /** The factory which creates new codecs. */
    private final CodecFactory factory;

    /** The codec cache. */
    private final Map<Class<?>, ValueCodec<?>> serializerMap = new HashMap<>();

    @SuppressWarnings("unchecked")
    public <T> ValueCodec<T> getCodec(Class<T> klass) {
        return (ValueCodec<T>) serializerMap.computeIfAbsent(klass, k -> factory.create(this, k));
    }

    @SuppressWarnings("unchecked")
    public <T, S extends ValueCodec<T>> ValueCodec<T> getCodec(Class<T> klass, BiFunction<CodecRegistry, Class<T>, S> factory) {
        return (ValueCodec<T>) serializerMap.computeIfAbsent(klass, k -> factory.apply(this, klass));
    }

    public CodecRegistry register(Class<?> klass, ValueCodec<?> serializer) {
        serializerMap.put(klass, serializer);
        return this;
    }

}
