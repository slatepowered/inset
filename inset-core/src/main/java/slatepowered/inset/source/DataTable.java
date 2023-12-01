package slatepowered.inset.source;

import slatepowered.inset.codec.EncodeOutput;
import slatepowered.inset.query.Query;

import java.util.concurrent.CompletableFuture;

/**
 * Represents a collection/table into a {@link DataSource}.
 */
public interface DataTable {

    /**
     * Get the data source for this table.
     *
     * @return The source.
     */
    DataSource getSource();

    /**
     * Synchronously update the given output data in the data table.
     *
     * @param output The output data.
     * @throws DataSourceException Any errors that may occur.
     */
    void updateOneSync(EncodeOutput output) throws DataSourceException;

    /**
     * Asynchronously updates the given output data in the data table.
     *
     * @param output The output data.
     * @return The result future.
     */
    default CompletableFuture<Void> updateOneAsync(final EncodeOutput output) {
        return CompletableFuture.runAsync(() -> this.updateOneSync(output));
    }

    /**
     * Find/load one item from the data table synchronously for
     * the given query.
     *
     * @param query The query.
     * @return The result.
     * @throws DataSourceException Any errors that may occur.
     */
    DataSourceQueryResult findOneSync(Query query) throws DataSourceException;

    /**
     * Find/load one item from the data table asynchronously for the
     * given query.
     *
     * @param query The query.
     * @return The query result future.
     */
    default CompletableFuture<DataSourceQueryResult> findOneAsync(final Query query) {
        return CompletableFuture.supplyAsync(() -> this.findOneSync(query));
    }

}
