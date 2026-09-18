package org.adrian.databinding;

/**
 * A scope that restores the previous active {@code DataBinder} name when closed. Returned by
 * {@link DataBinder#setActive(String)} for use in try-with-resources blocks. Closing is idempotent.
 */
public final class Scope implements AutoCloseable {

    private final String previousName;
    private boolean closed;

    /**
     * Creates a scope that restores the given active-binder name when closed.
     *
     * @param previousName the binder name to restore on {@link #close()}
     */
    Scope(final String previousName) {
        this.previousName = previousName;
    }

    @Override
    public void close() {
        if (!this.closed) {
            DataBinder.setActive(this.previousName);
            this.closed = true;
        }
    }
}
