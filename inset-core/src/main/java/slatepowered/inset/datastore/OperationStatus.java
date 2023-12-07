package slatepowered.inset.datastore;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import slatepowered.inset.query.FindStatus;
import slatepowered.inset.query.Query;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Common class to all operation statuses related to a {@link Datastore}.
 *
 * @param <K> The key type of the datastore.
 * @param <T> The value type of the datastore.
 * @param <R> The result type of the futures.
 */
@RequiredArgsConstructor
public abstract class OperationStatus<K, T, R extends OperationStatus<K, T, ?>> {

    @Getter
    protected final Datastore<K, T> datastore;

    @Getter
    protected final Query query;

    /**
     * Any errors if present, if present this means the operation failed.
     */
    protected Object error;

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
     * Get the error if the query failed.
     * This could be a {@link Throwable}, a string, an integer error code, etc.
     */
    public Object error() {
        return error;
    }

    /**
     * Get the error as the inferred type if the query failed.
     * This could be a {@link Throwable}, a string, an integer error code, etc.
     */
    @SuppressWarnings("unchecked")
    public <E> E errorAs() {
        return (E) error;
    }

    /**
     * Get the error as the given type if the query failed.
     * This could be a {@link Throwable}, a string, an integer error code, etc.
     */
    @SuppressWarnings("unchecked")
    public <E> E errorAs(Class<E> eClass) {
        return (E) error;
    }

    /**
     * Check if this operation failed, this simply checks whether
     * there is an error present so this will return false if the operation
     * has not completed yet.
     *
     * @return Whether the operation failed.
     */
    public boolean failed() {
        return error != null;
    }

    /**
     * Get a string which fits for the sentence 'while ...'
     * describing this operation.
     *
     * @return The description.
     */
    protected abstract String describeOperation();

    @SuppressWarnings("unchecked")
    private R castThis() {
        return (R) this;
    }

    /**
     * When the query fails, call the given consumer.
     *
     * @param consumer The consumer.
     * @return This.
     */
    public R exceptionally(Consumer<R> consumer) {
        future.whenComplete((status, throwable) -> {
            if (status != null && status.failed()) {
                consumer.accept(status);
            }
        });

        return castThis();
    }

    /**
     * Throw a {@link RuntimeException} describing the error if this operation failed.
     *
     * @return This.
     * @throws RuntimeException The exception.
     */
    public R throwIfFailed() {
        if (failed()) {
            Object error = error();
            Throwable cause = error instanceof Throwable ? errorAs() : null;
            throw new RuntimeException("Error while "  + describeOperation() +
                    (cause == null ? ": " + error : ""), cause);
        }

        return castThis();
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
