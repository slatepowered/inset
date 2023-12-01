package slatepowered.inset.codec;

import java.lang.reflect.Type;

/**
 * An input of structured data which is to be decoded.
 */
public abstract class DecodeInput {

    /**
     * Read the value of the given field name in the given context.
     *
     * @param context The context.
     * @param field The field.
     * @param expectedType The expected generic type.
     * @return The value of the field or null if absent.
     */
    public abstract Object read(CodecContext context, String field, Type expectedType);

    public abstract Object readKey(String field, Type expectedType);

    @SuppressWarnings("unchecked")
    public <R extends DecodeInput> R requireType(Class<R> rClass) {
        if (!rClass.isInstance(this))
            throw new RuntimeException("Input of type " + rClass.getName() + " required, got " + this.getClass().getSimpleName());
        return (R) this;
    }

}
