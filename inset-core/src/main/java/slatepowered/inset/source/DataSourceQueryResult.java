package slatepowered.inset.source;

import slatepowered.inset.codec.DecodeInput;
import slatepowered.inset.query.Query;

/**
 * Represents the result of a direct query from a data source.
 */
public interface DataSourceQueryResult {

    /**
     * The query which was executed and produced this result.
     *
     * @return The query.
     */
    Query getQuery();

    /**
     * Get whether an item was found in the data table.
     *
     * @return Whether an item was found.
     */
    boolean found();

    /**
     * Get the input for the data/body of the data result.
     *
     * This will be null if no item was found in the data table.
     *
     * @return The input or null if absent.
     */
    DecodeInput input();

}
