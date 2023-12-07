package slatepowered.inset.datastore;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import slatepowered.inset.query.Query;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Common class to all operation statuses related to a {@link Datastore}.
 *
 * @param <K> The key type of the datastore.
 * @param <T> The value type of the datastore.
 * @param <R> The result type of the futures.
 */
@RequiredArgsConstructor
public abstract class OperationStatus<K, T, R> {

    @Getter
    protected final Datastore<K, T> datastore;

    @Getter
    protected final Query query;

    /**
     * Whether the query completed at all, success is not required.
     */
    protected volatile boolean completed = false;

    /**
     * The base completable future.
     */
    protected final CompletableFuture<R> baseFuture = new CompletableFuture<>();

    /**
     * The current completable future.
     */
    protected CompletableFuture<R> future = baseFuture;

    /**
     * Called when all handlers on the above future have been executed.
     */
    protected final CompletableFuture<R> handledFuture = new CompletableFuture<>();

    /**
     * Get the awaitable future.
     *
     * @return The future.
     */
    public CompletableFuture<R> future() {
        return future;
    }

    /**
     * Modify and return the future for this query status, setting the
     * modified future as the current future on this query.
     *
     * @param action The editor action.
     * @return The future.
     */
    public CompletableFuture<R> future(Function<CompletableFuture<R>, CompletableFuture<R>> action) {
        future = action.apply(future);
        return future;
    }


    /**
     * The future called when all handlers on the above future have been executed.
     *
     * @return The handled future.
     */
    public CompletableFuture<R> handledFuture() {
        return handledFuture;
    }

    /**
     * Get whether this query completed at all.
     *
     * @return Whether it completed.
     */
    public boolean isCompleted() {
        return completed;
    }

    /**
     * Block this thread to await completion of the query.
     *
     * @return This.
     */
    public R await() {
        return future.join();
    }

    /**
     * Block this thread to await completion of the query and it's handlers.
     *
     * @return This.
     */
    public R awaitHandled() {
        return handledFuture.join();
    }

    /**
     * Complete this operation with the given value.
     *
     * @param value The value.
     */
    protected final void completeInternal(R value) {
        try {
            baseFuture.complete(value);
            handledFuture.complete(value);
        } catch (Throwable t) {
            handledFuture.completeExceptionally(t);
        }
    }

    /* TODO MAYBE */

}
