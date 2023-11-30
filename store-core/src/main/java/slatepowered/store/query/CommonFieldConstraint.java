package slatepowered.store.query;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * A field constraint commonly encountered in different data sources,
 * following the {@link CommonConstraintType} enum.
 *
 * @param <T> The value type.
 */
@RequiredArgsConstructor
@Getter
public abstract class CommonFieldConstraint<T> implements FieldConstraint<T> {

    protected final CommonConstraintType type;
    protected final T operand;

}
