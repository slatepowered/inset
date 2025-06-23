package slatepowered.inset.query.constraint;

import lombok.RequiredArgsConstructor;
import slatepowered.inset.util.Range;
import slatepowered.inset.util.ValueUtils;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Represents the operation/type of a common field constraint.
 */
@RequiredArgsConstructor
public enum CommonConstraintType {

    EQUAL {
        @Override
        public <T> CommonFieldConstraint<T> forOperand(Object operand) {
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
        public <T> CommonFieldConstraint<T> forOperand(Object operand) {
            return new CommonFieldConstraint<T>(NOT_EQUAL, operand) {
                @Override
                public boolean test(T t) {
                    return !Objects.equals(t, operand);
                }
            };
        }
    },
    GREATER {
        @Override
        public <T> CommonFieldConstraint<T> forOperand(Object operand) {
            if (operand == null)
                return new CommonFieldConstraint<T>(GREATER, null) {
                    @Override public boolean test(T t) { return true; }};
            final Double operandDouble = ((Number)operand).doubleValue();
            return new CommonFieldConstraint<T>(GREATER, operand) {
                @Override
                public boolean test(T t) {
                    return t != null && ((Number)t).doubleValue() > operandDouble;
                }
            };
        }
    },
    LESS {
        @Override
        public <T> CommonFieldConstraint<T> forOperand(Object operand) {
            if (operand == null)
                return new CommonFieldConstraint<T>(LESS, null) {
                    @Override public boolean test(T t) { return true; }};
            final Double operandDouble = ((Number)operand).doubleValue();
            return new CommonFieldConstraint<T>(LESS, operand) {
                @Override
                public boolean test(T t) {
                    return t != null && ((Number)t).doubleValue() < operandDouble;
                }
            };
        }
    },
    GREATER_OR_EQUAL {
        @Override
        public <T> CommonFieldConstraint<T> forOperand(Object operand) {
            if (operand == null)
                return new CommonFieldConstraint<T>(GREATER_OR_EQUAL, null) {
                    @Override public boolean test(T t) { return true; }};
            final Double operandDouble = ((Number)operand).doubleValue();
            return new CommonFieldConstraint<T>(GREATER_OR_EQUAL, operand) {
                @Override
                public boolean test(T t) {
                    return t != null && ((Number)t).doubleValue() >= operandDouble;
                }
            };
        }
    },
    LESS_OR_EQUAL {
        @Override
        public <T> CommonFieldConstraint<T> forOperand(Object operand) {
            if (operand == null)
                return new CommonFieldConstraint<T>(LESS_OR_EQUAL, null) {
                    @Override public boolean test(T t) { return true; }};
            final Double operandDouble = ((Number)operand).doubleValue();
            return new CommonFieldConstraint<T>(LESS_OR_EQUAL, operand) {
                @Override
                public boolean test(T t) {
                    return t != null && ((Number)t).doubleValue() <= operandDouble;
                }
            };
        }
    },
    EXISTS {
        @Override
        public <T> CommonFieldConstraint<T> forOperand(Object operand) {
            return new CommonFieldConstraint<T>(EXISTS, operand) {
                @Override
                public boolean test(T t) {
                    return true; // if this constraint is called the field always exists
                }
            };
        }
    },
    ONE_OF {
        @Override
        public <T> CommonFieldConstraint<T> forOperand(Object operand) {
            // compile operand to a HashSet
            Set<Object> set = ValueUtils.ensureSet(operand);
            return new CommonFieldConstraint<T>(ONE_OF, set) {
                @Override
                public boolean test(T t) {
                    return set.contains(t);
                }
            };
        }
    },
    IN_RANGE {
        @Override
        public <T> CommonFieldConstraint<T> forOperand(Object operand) {
            // compile operand to a HashSet
            Range range = (Range) operand;
            return new CommonFieldConstraint<T>(IN_RANGE, range) {
                @Override
                public boolean test(T t) {
                    return range.contains(((Number)t).longValue());
                }
            };
        }
    }

    // TODO: GREATER, LESS, GREATER_OR_EQUAL, LESS_OR_EQUAL

    ;

    /**
     * Create a new common field constraint for the given operand.
     *
     * @param operand The operand.
     * @param <T> The value type.
     * @return The constraint.
     */
    public abstract <T> CommonFieldConstraint<T> forOperand(Object operand);

}
