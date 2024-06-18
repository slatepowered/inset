package slatepowered.inset.source;

import slatepowered.inset.DataManager;
import slatepowered.inset.codec.EncodeOutput;

import java.util.Collection;
import java.util.concurrent.ExecutorService;

/**
 * Represents a type of data storage.
 */
public interface DataSource {

    /**
     * Get all opened tables on this data source.
     *
     * @return The collection of data tables.
     */
    Collection<? extends DataTable> allTables();

    /**
     * Get the executor service to use for scheduling operations
     * on this data source.
     *
     * @return The executor.
     */
    default ExecutorService getExecutorService() {
        return getDataManager().getExecutorService();
    }

    /**
     * Get the data manager for this source.
     *
     * @return The data manager.
     */
    DataManager getDataManager();

    /**
     * Creates a new document serialization output which produces
     * a result compatible with this data source.
     *
     * @return The output.
     */
    EncodeOutput createDocumentSerializationOutput();

    /**
     * Get or create a new table/collection with the given name.
     *
     * @param name The name.
     * @return The table/collection.
     */
    DataTable table(String name);

}
