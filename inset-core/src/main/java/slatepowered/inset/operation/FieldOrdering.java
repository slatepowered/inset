package slatepowered.inset.operation;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Represents the ordering of a field.
 */
@RequiredArgsConstructor
@Getter
public enum FieldOrdering {

    ASCENDING(1),

    DESCENDING(-1)

    ;

    protected final int factor;

}
