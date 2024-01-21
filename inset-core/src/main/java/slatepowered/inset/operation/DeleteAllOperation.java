package slatepowered.inset.operation;

import lombok.Getter;
import slatepowered.inset.datastore.Datastore;
import slatepowered.inset.datastore.OperationStatus;
import slatepowered.inset.query.Query;

/**
 * Describes the deletion of all items in a datastore matching a certain filter.
 *
 * @param <K> The key type.
 * @param <T> The data value type.
 */
public class DeleteAllOperation<K, T> extends OperationStatus<K, T, DeleteAllOperation<K, T>> {

    /**
     * The count of deleted items.
     */
    @Getter
    protected volatile Long deleteCount;

    /**
     * Whether the data table operation was completed.
     */
    protected volatile boolean completedDataTableOperation;

    /**
     * Whether the cache clear was completed successfully.
     */
    protected volatile boolean completedCacheClear;

    public DeleteAllOperation(Datastore<K, T> datastore, Query query) {
        super(datastore, query);
    }

    @Override
    protected String describeOperation() {
        return "delete all";
    }

    /**
     * Complete this query with the given parameters.
     */
    public synchronized void completeDataTableOperation(Object error, Long deleteCount) {
        this.error = error;
        this.deleteCount = deleteCount;

        completedDataTableOperation = true;
        if (completedCacheClear) {
            completeInternal(this);
        }
    }

    public synchronized void completeCacheClear() {
        this.completedCacheClear = true;
        if (completedDataTableOperation) {
            completeInternal(this);
        }
    }

}
