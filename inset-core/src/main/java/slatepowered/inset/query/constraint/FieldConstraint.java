package slatepowered.inset.query.constraint;

import java.util.function.Predicate;

/**
 * A constraint/condition/comparator on the values of a field in a query.
 */
public interface FieldConstraint<T> extends Predicate<T> {

}
