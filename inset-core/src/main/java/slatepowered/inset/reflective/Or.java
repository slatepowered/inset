package slatepowered.inset.reflective;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Migration policy if the key associated with this field does not exist.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.RECORD_COMPONENT })
public @interface Or {

  /**
   * Special variants:
   * - `ignoreAbsent` ignores null values from the decoding chain, avoiding overwriting an existent value with null
   * - `createDefaultIfNull` if the final value on the field is null, it will try to create a default value by decoding an empty input
   * - The combined variant combines these behaviors.
   */
  String value() default "declineAbsent+decodeDefaultIfNull";

}
