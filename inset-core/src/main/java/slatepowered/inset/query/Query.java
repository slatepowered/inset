package slatepowered.inset.query;

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

    static Query key(Object key) {
        return new Query() {
            // The cached field map
            Map<String, FieldConstraint<?>> fieldConstraintMap;

            @Override
            public boolean hasKey() {
                return true;
            }

            @Override
            public Object getKey() {
                return key;
            }

            @Override
            public FieldConstraint<?> getConstraint(String name) {
                return null; // TODO
            }

            @Override
            public Map<String, FieldConstraint<?>> getFieldConstraints() {
                return fieldConstraintMap; // TODO
            }
        };
    }

}
