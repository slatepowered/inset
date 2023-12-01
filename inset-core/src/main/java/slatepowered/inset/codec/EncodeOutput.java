package slatepowered.inset.codec;

/**
 * An output for structured data which values are to be encoded into.
 */
public abstract class EncodeOutput {

    // The key object
    protected Object key;

    /**
     * Set the primary key for the output data.
     *
     * @param name The field name of the key.
     * @param key The key.
     */
    public void setKey(CodecContext context, String name, Object key) {
        this.key = key;
        registerKey(context, name, key);
    }

    // Register a key change in the serialized data
    protected abstract void registerKey(CodecContext context, String name, Object key);

    /**
     * Register the given value for the given field name/key in this serialization output.
     *
     * @param context The context.
     * @param field The field name.
     * @param value The value.
     */
    public abstract void set(CodecContext context, String field, Object value);

    /**
     * Build the output data into a final database compatible object.
     *
     * @return The object.
     */
    public abstract Object build();

}
