package org.adrian.databinding;

/**
 * Test-only utility that provides {@code reset} and {@code drainOnce} operations for the active {@link DataBinder}
 * without polluting the production API.
 */
final class TestDataBinder {

    private TestDataBinder() {}

    /**
     * Clears all binding registrations and phantom-reference tracking on the active {@link DataBinder} instance.
     * Intended for test setup/teardown only.
     */
    static void reset() {
        DataBinder.getActive().clearAll();
    }

    /**
     * Drains all currently-enqueued phantom references on the shared cleaner synchronously, processing each one the
     * same way the background daemon does. Allows tests to flush cleanup without relying on the daemon thread's
     * latency.
     *
     * @return the number of phantom references processed
     */
    static int drainOnce() {
        return DataBinderCleaner.getInstance().drainQueue();
    }
}
