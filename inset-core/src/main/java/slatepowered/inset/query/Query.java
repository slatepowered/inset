package slatepowered.inset.query;

import slatepowered.inset.codec.DataCodec;
import slatepowered.inset.datastore.Datastore;
import slatepowered.inset.query.constraint.CommonConstraintType;
import slatepowered.inset.query.constraint.CommonFieldConstraint;
import slatepowered.inset.query.constraint.FieldConstraint;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a query into a datastore.
 */
public interface Query {

    /**
     * Whether this query searched on a primary key.
     */
    boolean hasKey();

    /**
     * Get the primary key this query searches on.
     */
    Object getKey();

    /**
     * Get the name of the key field if applicable.
     *
     * @return The key field name.
     */
    String getKeyField();

    /**
     * Get the constraint for the given field name.
     *
     * @param name The name.
     * @return The constraint..
     */
    FieldConstraint<?> getConstraint(String name);

    /**
     * Get the constrained fields and their constraints for this query.
     *
     * @return The fields.
     */
    Map<String, FieldConstraint<?>> getFieldConstraints();

    /**
     * Qualifies this query for the given data store.
     *
     * @param datastore The datastore.
     * @return This.
     */
    default Query qualify(Datastore<?, ?> datastore) {
        return this;
    }

    static Query key(final Object key) {
        return new Query() {
            // The cached field map
            Map<String, FieldConstraint<?>> fieldConstraintMap;
            FieldConstraint<?> constraint;
            DataCodec<?, ?> dataCodec;

            // ensure the constraints are created and
            // registered for when we need them
            private void ensureConstraints() {
                if (constraint == null) {
                    constraint = CommonConstraintType.EQUAL.forOperand(key);
                }

                if (fieldConstraintMap == null) {
                    fieldConstraintMap = new HashMap<>();
                    fieldConstraintMap.put(getKeyField(), constraint);
                }
            }

            @Override
            public boolean hasKey() {
                return true;
            }

            @Override
            public Object getKey() {
                return key;
            }

            @Override
            public String getKeyField() {
                return dataCodec.getPrimaryKeyFieldName();
            }

            @Override
            public FieldConstraint<?> getConstraint(String name) {
                ensureConstraints();
                return name.equals(getKeyField()) ? constraint : null;
            }

            @Override
            public Map<String, FieldConstraint<?>> getFieldConstraints() {
                ensureConstraints();
                return fieldConstraintMap;
            }

            @Override
            public Query qualify(Datastore<?, ?> datastore) {
                dataCodec = datastore.getDataCodec();
                return this;
            }
        };
    }

    /**
     * Creates a new field constraint-based query for the given
     * field constraints.
     *
     * @param fieldConstraintMap The field constraints.
     * @return The query.
     */
    static Query forFields(Map<String, FieldConstraint<?>> fieldConstraintMap) {
        return new Query() {
            // Cached hasKey field, can be checked reliably
            // once it is qualified
            Boolean hasKey;

            // Cached key
            Object key;

            // The datastore this query was qualified for
            Datastore<?, ?> datastore;

            @Override
            public boolean hasKey() {
                if (hasKey == null) {
                    String field = getKeyField();
                    hasKey = field != null && fieldConstraintMap.containsKey(field);
                }

                return hasKey;
            }

            @Override
            public Object getKey() {
                if (key == null) {
                    String field = getKeyField();
                    if (field == null) {
                        return null;
                    }

                    // find key from field constraints
                    FieldConstraint<?> constraint = fieldConstraintMap.get(field);
                    if (constraint == null) {
                        return null;
                    }

                    if (constraint instanceof CommonFieldConstraint) {
                        return key = ((CommonFieldConstraint<?>)constraint).getOperand();
                    }
                }

                return key;
            }

            @Override
            public String getKeyField() {
                return datastore != null ? datastore.getDataCodec().getPrimaryKeyFieldName() : null;
            }

            @Override
            public FieldConstraint<?> getConstraint(String name) {
                return fieldConstraintMap.get(name);
            }

            @Override
            public Map<String, FieldConstraint<?>> getFieldConstraints() {
                return Collections.unmodifiableMap(fieldConstraintMap);
            }

            @Override
            public Query qualify(Datastore<?, ?> datastore) {
                key = null;
                hasKey = null;
                this.datastore = datastore;
                return this;
            }
        };
    }

    static Builder builder() {
        return new Builder();
    }

    /**
     * Builds queries with field constraints.
     */
    class Builder {
        private Map<String, FieldConstraint<?>> fieldConstraintMap = new HashMap<>();

        public Builder constrain(String field, FieldConstraint<?> constraint) {
            fieldConstraintMap.put(field, constraint);
            return this;
        }

        public Builder eq(String field, Object value) {
            return constrain(field, CommonConstraintType.EQUAL.forOperand(value));
        }

        public Builder neq(String field, Object value) {
            return constrain(field, CommonConstraintType.NOT_EQUAL.forOperand(value));
        }

        public Builder greater(String field, Object value) {
            return constrain(field, CommonConstraintType.GREATER.forOperand(value));
        }

        public Builder less(String field, Object value) {
            return constrain(field, CommonConstraintType.LESS.forOperand(value));
        }

        public Builder greaterOrEq(String field, Object value) {
            return constrain(field, CommonConstraintType.GREATER_OR_EQUAL.forOperand(value));
        }

        public Builder lessOrEq(String field, Object value) {
            return constrain(field, CommonConstraintType.LESS_OR_EQUAL.forOperand(value));
        }

        public Builder exists(String field) {
            return constrain(field, CommonConstraintType.EXISTS.forOperand(/* no operand */ null));
        }

        public Builder oneOf(String field, Object... values) {
            return constrain(field, CommonConstraintType.ONE_OF.forOperand(values));
        }

        public Builder oneOf(String field, Collection<Object> values) {
            return constrain(field, CommonConstraintType.ONE_OF.forOperand(values));
        }

        public Query build() {
            return forFields(fieldConstraintMap);
        }
    }

}
