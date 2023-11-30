package slatepowered.store.serializer;

/**
 *
 */
public abstract class SerializationOutput {

    // The key object
    protected Object key;

    /**
     * Set the primary key for the output data.
     *
     * @param key The key.
     */
    public void setKey(Object key) {
        this.key = key;
        registerKey(key);
    }

    // Register a key change in the serialized data
    protected abstract void registerKey(Object key);

    /**
     * Register the given value for the given field name/key in this serialization output.
     *
     * @param context The context.
     * @param field The field name.
     * @param value The value.
     */
    public abstract void set(SerializationContext context, String field, Object value);

    /**
     * Build the output data into a final database compatible object.
     *
     * @return The object.
     */
    public abstract Object build();

}
