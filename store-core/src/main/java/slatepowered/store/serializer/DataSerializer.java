package slatepowered.store.serializer;

/**
 * Serializes/deserializes data of type {@code T} to/from serialization
 * outputs and inputs.
 *
 * @param <T> The document/value type.
 */
public interface DataSerializer<T> {

    /**
     * Serialize the given value into the given serialization output.
     *
     * @param context The context.
     * @param value The input value.
     * @param output The output.
     */
    void serialize(SerializationContext context, T value, SerializationOutput output);

    /**
     * Construct a value of the correct type from the given serialization input.
     *
     * @param context The context.
     * @param input The input.
     * @return The output value.
     */
    T construct(SerializationContext context, SerializationInput input);

    /**
     * Deserialize the fields of the given document instance from the given input.
     *
     * @param context The context.
     * @param instance The output document/value.
     * @param input The input.
     */
    void deserialize(SerializationContext context, T instance, SerializationInput input);

}
