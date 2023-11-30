package slatepowered.store.query;

import java.util.function.Predicate;

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
     * Get or create the query model for this query.
     *
     * @return The model.
     */
    QueryModel getModel();

    static Query key(Object key) {
        return new Query() {
            // The cached QueryModel
            QueryModel queryModelCache;

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
            public QueryModel getModel() {
                if (queryModelCache == null) {
                    queryModelCache = null; // TODO
                }

                return queryModelCache;
            }
        };
    }

}
