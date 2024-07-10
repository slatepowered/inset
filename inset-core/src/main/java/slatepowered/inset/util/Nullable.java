package slatepowered.inset.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a field is nullable.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Nullable {

    /**
     * The flag set on a field when it is nullable.
     */
    long FLAG = 1 >> 1;

}
