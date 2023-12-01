package slatepowered.inset.codec;

import lombok.Getter;

/**
 * An output for structured data which values are to be encoded into.
 */
public abstract class EncodeOutput {

    // The key object
    @Getter
    protected Object setKey;

    // The set key field
    @Getter
    protected String setKeyField;

    /**
     * Set the primary key for the output data.
     *
     * @param name The field name of the key.
     * @param key The key.
     */
    public void setSetKey(CodecContext context, String name, Object key) {
        this.setKey = key;
        this.setKeyField = name;
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

    @SuppressWarnings("unchecked")
    public <R extends EncodeOutput> R requireType(Class<R> rClass) {
        if (!rClass.isInstance(this))
            throw new RuntimeException("Output of type " + rClass.getName() + " required, got " + this.getClass().getSimpleName());
        return (R) this;
    }

}
