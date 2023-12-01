package slatepowered.inset.codec;

import java.lang.reflect.Type;

/**
 * An input of structured data which is to be decoded.
 */
public abstract class DecodeInput {

    // The key which was read.
    protected Object readKey;

    /**
     * Read the value of the given field name in the given context.
     *
     * @param context The context.
     * @param field The field.
     * @param expectedType The expected generic type.
     * @return The value of the field or null if absent.
     */
    public abstract Object read(CodecContext context, String field, Type expectedType);

    /**
     * Get or read the key from this input.
     *
     * @param field The key field name.
     * @param expectedType The expected type.
     * @return The key.
     */
    public Object getOrReadKey(String field, Type expectedType) {
        if (readKey == null) {
            readKey = readKey(field, expectedType);
        }

        return readKey;
    }

    /**
     * Read the key from this input.
     *
     * @param field The key field name.
     * @param expectedType The expected type.
     * @return The read key.
     */
    protected abstract Object readKey(String field, Type expectedType);

    @SuppressWarnings("unchecked")
    public <R extends DecodeInput> R requireType(Class<R> rClass) {
        if (!rClass.isInstance(this))
            throw new RuntimeException("Input of type " + rClass.getName() + " required, got " + this.getClass().getSimpleName());
        return (R) this;
    }

}
