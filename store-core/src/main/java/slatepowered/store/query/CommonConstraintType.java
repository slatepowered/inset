package slatepowered.store.query;

import lombok.RequiredArgsConstructor;

import java.util.Objects;

/**
 * Represents the operation/type of a common field constraint.
 */
@RequiredArgsConstructor
public enum CommonConstraintType {

    EQUAL {
        @Override
        public <T> CommonFieldConstraint<T> forOperand(T operand) {
            return new CommonFieldConstraint<T>(EQUAL, operand) {
                @Override
                public boolean test(T t) {
                    return Objects.equals(operand, t);
                }
            };
        }
    },
    NOT_EQUAL {
        @Override
        public <T> CommonFieldConstraint<T> forOperand(T operand) {
            return new CommonFieldConstraint<T>(NOT_EQUAL, operand) {
                @Override
                public boolean test(T t) {
                    return !Objects.equals(t, operand);
                }
            };
        }
    },

    // TODO: GREATER, LESS, GREATER_OR_EQUAL, LESS_OR_EQUAL

    ;

    /**
     * Create a new common field constraint for the given operand.
     *
     * @param operand The operand.
     * @param <T> The value type.
     * @return The constraint.
     */
    public abstract <T> CommonFieldConstraint<T> forOperand(T operand);

}
