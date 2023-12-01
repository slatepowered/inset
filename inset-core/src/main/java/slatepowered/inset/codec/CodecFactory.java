package slatepowered.inset.codec;

/**
 * Produces {@link ValueCodec}s at runtime.
 */
@FunctionalInterface
public interface CodecFactory {

    /**
     * Create a new initial codec for the given class in
     * context of the provided registry.
     *
     * @param registry The registry.
     * @param klass The class to serialize.
     * @param <T> The serialized type.
     * @return The codec.
     */
    <T> ValueCodec<T> create(CodecRegistry registry, Class<T> klass);

}
