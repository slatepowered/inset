package slatepowered.inset.reflective;

import slatepowered.inset.codec.CodecContext;
import slatepowered.inset.codec.DecodeInput;
import slatepowered.inset.util.Special;

import java.lang.reflect.Type;

public interface FieldAccessor {

  /**
   * Get the current value from the given instance as an object,
   * boxing primitive types.
   */
  Object getAsObject(Object instance);

  /**
   * Set the given object value for this field on the provided instance,
   * unboxing given primitives if needed.
   */
  void setFromObject(Object instance, Object value);

  /**
   * Get the canonical name as defined in the class.
   */
  String getName();

  /**
   * Get the effective serialized name. This may not return null, if the name isn't overridden
   * it should return the canonical name.
   */
  String getSerializedName();

  /**
   * Get the (generic) type of the field.
   */
  Type getType();

  /**
   * Get the non-generic value class of the field.
   */
  Class<?> getTypeAsClass();

  /**
   * Get the primitive type of the field, see the {@link UnsafeFieldDesc} `PT` constants.
   */
  byte getPrimitiveType();

  /**
   * Get miscellaneous flags which may be related to encoding or decoding, such as
   * {@link slatepowered.inset.util.NotNullable#FLAG}.
   */
  long getFlags();

  /**
   * Whether this field may be transient, that is, whether the type
   * inherits the {@link slatepowered.inset.codec.support.PotentiallyTransient} interface.
   */
  boolean isPotentiallyTransient();

  /**
   * By what name to mark the decoding frame if applicable. Null disables marking.
   */
  String getMark();

  /**
   * Get the policy for defaulting the value, as usually declared by the {@link Or} annotation
   */
  ValuePolicy getValuePolicy();

  /**
   * Whether the (recently decoded) value on this field must be processed.
   */
  boolean mustProcessDecoding();

  @SuppressWarnings("unchecked")
  default Object decodeAndConform(CodecContext context, DecodeInput input, Object parent) {
    // the original value of the field. this should be tracked because
    // we dont want to apply default policy updates twice since it
    // already occurs during the decoding process.
    Object original = parent != null ? getAsObject(parent) : Special.IGNORE;
    Object current = original; // track the current value set on the field

    Object read = input.read(context, getSerializedName(), getType());

    // check for a decoded value policy
    ValuePolicy policy = getValuePolicy();
    if (policy != null) {
      if (policy.shouldAcceptValue(context, this, parent, read)) {
        current = read;
      }

      if (policy.shouldUpdateValue(context, this, parent, current)) {
        current = policy.updateValue(context, this, parent, current);
      } else {
        // if we remain at our original value of not null, we must invoke `updateNotDecoded`.
        // if we accepted the read value, its decoding pipeline will have updated the fields as necessary
        if (original == current && current != null) {
          context.findCodec((Class<Object>) getTypeAsClass()).updateNotDecoded(context, current);
        }
      }
    } else {
      // otherwise, just accept the read object and move on.
      // the read value must have come from the decoding pipeline, so there
      // is no need to invoke updateNotDecoded
      current = read;
    }

    return current;
  }

  // Should only be called on fields that didn't undergo the decoding procedure
  @SuppressWarnings("unchecked")
  default Object updateNotDecoded(CodecContext context, Object parent) {
    ValuePolicy policy = getValuePolicy();
    if (policy == null) {
      return Special.IGNORE;
    }

    Object current = getAsObject(parent);
    if (policy.shouldUpdateValue(context, this, parent, current)) {
      current = policy.updateValue(context, this, parent, getAsObject(parent));
    }

    if (current != null) {
      context.findCodec((Class<Object>) current.getClass()).updateNotDecoded(context, current);
    }

    return current;
  }

}
