package slatepowered.inset.query;

import slatepowered.inset.codec.DataCodec;
import slatepowered.inset.datastore.Datastore;

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

    static Query key(Object key) {
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

}
