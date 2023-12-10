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
    void replaceOneSync(EncodeOutput output) throws DataSourceException;

    /**
     * Asynchronously updates the given output data in the data table.
     *
     * @param output The output data.
     * @return The result future.
     */
    default CompletableFuture<Void> replaceOneAsync(final EncodeOutput output) {
        return CompletableFuture.runAsync(() -> this.replaceOneSync(output), getSource().getDataManager().getExecutorService());
    }

    /**
     * Find/load one item from the data table synchronously for
     * the given query.
     *
     * @param query The query.
     * @return The result.
     * @throws DataSourceException Any errors that may occur.
     */
    DataSourceFindResult findOneSync(Query query) throws DataSourceException;

    /**
     * Find/load one item from the data table asynchronously for the
     * given query.
     *
     * @param query The query.
     * @return The query result future.
     */
    default CompletableFuture<DataSourceFindResult> findOneAsync(final Query query) {
        return CompletableFuture.supplyAsync(() -> this.findOneSync(query), getSource().getDataManager().getExecutorService());
    }

    /**
     * Find/load multiple items from the data table synchronously for
     * the given query.
     *
     * @param query The query.
     * @return The result.
     * @throws DataSourceException Any errors that may occur.
     */
    DataSourceBulkIterable findAllSync(Query query) throws DataSourceException;

    /**
     * Find/load multiple items from the data table asynchronously for
     * the given query.
     *
     * @param query The query.
     * @return The result.
     */
    default CompletableFuture<DataSourceBulkIterable> findAllAsync(final Query query) {
        return CompletableFuture.supplyAsync(() -> this.findAllSync(query), getSource().getDataManager().getExecutorService());
    }

    /**
     * Delete the first item matching the given query.
     *
     * @param query THe query.
     * @return Whether the item was successfully deleted.
     */
    boolean deleteOne(Query query);

}
