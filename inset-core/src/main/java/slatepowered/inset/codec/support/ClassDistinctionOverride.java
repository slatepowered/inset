package slatepowered.inset.codec.support;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ClassDistinctionOverride {
    /**
     * The encoded field name for this distinction.
     */
    String value();
}
