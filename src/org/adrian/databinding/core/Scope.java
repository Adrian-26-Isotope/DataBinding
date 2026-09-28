package org.adrian.databinding.core;

import java.util.function.Consumer;

/**
 * A scope that restores the previous active {@code DataBinder} name when closed. Returned by
 * {@link DataBinder#setActive(String)} for use in try-with-resources blocks. Closing is idempotent.
 */
public final class Scope implements AutoCloseable {

    private final String previousName;
    private boolean closed;
    private final Consumer<String> closeConsumer;

    /**
     * Creates a scope that restores the given active-binder name when closed.
     *
     * @param previousName the binder name to restore on {@link #close()}
     */
    Scope(final String previousName, final Consumer<String> closeConsumer) {
        this.previousName = previousName;
        this.closeConsumer = closeConsumer;
    }

    @Override
    public void close() {
        if (!this.closed) {
            this.closeConsumer.accept(this.previousName);
            this.closed = true;
        }
    }
}
