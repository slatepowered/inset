package slatepowered.inset.modifier;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Represents a common, simple type of projection.
 */
@RequiredArgsConstructor
@Getter
public class CommonProjection implements Projection {

    /**
     * Specifies the action to perform with the field list operand.
     */
    public enum Action {

        /**
         * Include the set fields.
         */
        INCLUDE,

        /**
         * Exclude the set fields.
         */
        EXCLUDE

    }

    protected final Action action;
    protected final List<String> fieldNames;

}
