package slatepowered.inset;

import lombok.Builder;
import lombok.Getter;
import slatepowered.inset.codec.CodecRegistry;
import slatepowered.inset.codec.DataCodec;
import slatepowered.inset.datastore.Datastore;
import slatepowered.inset.source.DataTable;

import java.util.concurrent.*;

/**
 * Manages all resources related to datastores, -sources, etc.
 */
@Builder
@Getter
public class DataManager {

    static final int DEFAULT_AWAIT_TIMEOUT_SECONDS = 30;

    /**
     * The general codec registry.
     */
    protected final CodecRegistry codecRegistry;

    /**
     * The executor to use for asynchronous operations.
     */
    protected final ExecutorService executorService;

    /**
     * Await all ongoing queries and their handlers to finish
     * before continuing this thread.
     *
     * @return True if all tasks are complete, false if it was interrupted or timed out.
     */
    public boolean await() {
        try {
            return executorService.awaitTermination(DEFAULT_AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
            return false;
        }
    }

    /**
     * Await all ongoing queries and their handlers to finish
     * before continuing this thread, with the given maximum timeout time.
     *
     * @param timeout The timeout time in the given unit.
     * @param timeUnit The timeout time unit.
     * @return True if all tasks are complete, false if it was interrupted or timed out.
     */
    public boolean await(int timeout, TimeUnit timeUnit) {
        try {
            return executorService.awaitTermination(timeout, timeUnit);
        } catch (InterruptedException ignored) {
            return false;
        }
    }

    /**
     * Create a new datastore from the given table for the
     * given key and value types using this codec registry.
     *
     * @param <K> The key type.
     * @param <T> The value type.
     * @return The datastore.
     */
    public <K, T> Datastore.DatastoreBuilder<K, T> datastore(
            Class<K> kClass,
            Class<T> tClass
    ) {
        return Datastore.<K, T>builder()
                .dataManager(this)
                .keyClass(kClass)
                .dataCodec((DataCodec<K, T>) codecRegistry.getCodec(tClass));
    }

}
