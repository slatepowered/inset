package slatepowered.inset.codec;

/**
 * Serializes/deserializes data of type {@code T} to/from serialization
 * outputs and inputs.
 *
 * @param <T> The document/value type.
 */
public interface ValueCodec<T> {

    /**
     * Expect this instance to be of a specific {@link ValueCodec} super type.
     *
     * @param cClass The expected class.
     * @param <C> The return type.
     * @return This.
     */
    @SuppressWarnings("unchecked")
    default <C extends ValueCodec<T>> C expect(Class<? super C> cClass) {
        if (!cClass.isInstance(this)) {
            throw new IllegalArgumentException("Expected " + cClass.getSimpleName() + ", got " + this.getClass().getSimpleName() + " (maybe you forgot to define a primary key?)");
        }

        return (C) this;
    }

    /**
     * Serialize the given value into the given serialization output.
     *
     * @param context The context.
     * @param value The input value.
     * @param output The output.
     */
    void encode(CodecContext context, T value, EncodeOutput output);

    /**
     * Construct a value of the correct type from the given serialization input.
     *
     * @param context The context.
     * @param input The input.
     * @return The output value.
     */
    T construct(CodecContext context, DecodeInput input);

    /**
     * Deserialize the fields of the given document instance from the given input.
     *
     * @param context The context.
     * @param instance The output document/value.
     * @param input The input.
     */
    void decode(CodecContext context, T instance, DecodeInput input);

    /**
     * Get the field on the given instance.
     *
     * @param instance The instance.
     * @param field The field.
     * @param <V> The field value type.
     * @return The value of the field or null if absent.
     */
    <V> V getField(T instance, String field);

    default T constructAndDecode(CodecContext context, DecodeInput input) {
        T instance = construct(context, input);
        decode(context, instance, input);
        return instance;
    }

}
