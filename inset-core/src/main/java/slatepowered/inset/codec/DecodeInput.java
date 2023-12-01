package slatepowered.inset.codec;

/**
 * An input of structured data which is to be decoded.
 */
public abstract class DecodeInput {

    /**
     * Read the value of the given field name in the given context.
     *
     * @param context The context.
     * @param field The field.
     * @return The value of the field or null if absent.
     */
    public abstract Object read(CodecContext context, String field);

}
