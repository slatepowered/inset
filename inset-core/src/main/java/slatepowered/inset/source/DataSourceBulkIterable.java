package slatepowered.inset.source;

import slatepowered.inset.modifier.Projection;
import slatepowered.inset.modifier.Sorting;
import slatepowered.inset.query.FoundItem;
import slatepowered.inset.query.Query;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Represents the result of a direct find-all/many operation on a data source.
 *
 * This doesn't have to mean a completed result, it just has to provide
 * a means of retrieving the items returned by a query and modifying the projections.
 */
public interface DataSourceBulkIterable extends DataSourceOperation {

    /**
     * Get the query.
     *
     * @return The query.
     */
    Query getQuery();

    /**
     * Set the batch size to use for retrieving the data from the server.
     *
     * @param size The batch size.
     * @return This.
     */
    DataSourceBulkIterable batch(int size);

    /**
     * Further filter the results in this iterable.
     *
     * @param query The filter query.
     * @return This.
     */
    DataSourceBulkIterable filter(Query query);

    /**
     * Limit the amount of results this set may return.
     *
     * @param limit The limit.
     * @return This.
     */
    DataSourceBulkIterable limit(int limit);

    /**
     * Apply the given projection to this iterable query, this will cause
     * the data to be partial so to deserialize full objects it will need
     * to be fetched from the database.
     *
     * @param projection The projection to apply.
     * @return This.
     */
    DataSourceBulkIterable projection(Projection projection);

    /**
     * Sort this the results of this iterable.
     *
     * @param sorting The sorting to apply.
     * @return This.
     */
    DataSourceBulkIterable sort(Sorting sorting);

    /**
     * Get the first item in this result set.
     *
     * @return The first item or empty if absent.
     */
    Optional<FoundItem<?, ?>> first();

    /**
     * Get the next item in this result set.
     *
     * This will be the first item if the cursor has not been moved.
     *
     * @return The next item.
     */
    Optional<FoundItem<?, ?>> next();

    /**
     * Check whether there is another item.
     *
     * @return Whether there is a next item.
     */
    boolean hasNext();

    /**
     * Get a list of all items in this result set.
     *
     * @return The list of items.
     */
    List<FoundItem<?, ?>> list();

    /**
     * Stream all items in this result set.
     *
     * @return The stream.
     */
    Stream<FoundItem<?, ?>> stream();

}
