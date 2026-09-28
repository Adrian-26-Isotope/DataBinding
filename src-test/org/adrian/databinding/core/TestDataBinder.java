package org.adrian.databinding.core;

import java.util.UUID;

/**
 * Test-only utility that provides {@code reset} and {@code drainOnce} operations for the active {@link DataBinder}
 * without polluting the production API.
 */
public final class TestDataBinder {

    private TestDataBinder() {}

    /**
     * Clears all binding registrations and phantom-reference tracking on the active {@link DataBinder} instance.
     * Intended for test setup/teardown only.
     */
    public static void reset() {
        DataBinder.getActive().clearAll();
    }

    /**
     * Drains all currently-enqueued phantom references on the shared cleaner synchronously, processing each one the
     * same way the background daemon does. Allows tests to flush cleanup without relying on the daemon thread's
     * latency.
     *
     * @return the number of phantom references processed
     */
    public static int drainOnce() {
        return DataBinderCleaner.getInstance().drainQueue();
    }

    /**
     * Directly invokes {@code cleanupReceiver} on the active {@link DataBinder} for the specified receiver UUID,
     * bypassing the phantom-reference/GC mechanism. Allows tests to exercise the {@code bind()}/{@code cleanup()}
     * race deterministically without relying on garbage-collection timing.
     *
     * @param receiverId the UUID of the receiver whose binding entries should be cleaned up
     */
    public static void forceCleanupReceiver(final UUID receiverId) {
        DataBinder.getActive().cleanupReceiver(receiverId);
    }
}
