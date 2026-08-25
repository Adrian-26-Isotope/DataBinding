package org.adrian.databinding;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Handles automatic cleanup of {@link DataBinder} entries when {@link IBindable} objects are garbage collected. Uses
 * PhantomReference to detect when objects become unreachable and removes their UUID mappings from the owning
 * {@link DataBinder} cache.
 * <p>
 * Each instance is owned by a single {@link DataBinder} and holds a back-reference so it can invoke the owner's cleanup
 * methods when phantom references are enqueued. The background daemon thread is started when the cleaner is constructed
 * (i.e. when the owning {@link DataBinder} is first created) and can be stopped via {@link #shutdown()}.
 * </p>
 */
class DataBinderCleaner {

    private final DataBinder owner;
    private final ReferenceQueue<IBindable> referenceQueue = new ReferenceQueue<>();
    private final ConcurrentMap<PhantomReference<IBindable>, UUID> transmitterMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<PhantomReference<IBindable>, UUID> receiverMap = new ConcurrentHashMap<>();

    private volatile boolean shutdownRequested;

    private static final long POLL_TIMEOUT_MS = 1000L;

    private final RestartGuard restartGuard = new RestartGuard();

    /**
     * Constructs a cleaner owned by the specified {@link DataBinder} and starts the background daemon thread.
     *
     * @param owner the {@link DataBinder} that owns this cleaner; used for cleanup callbacks
     * @param name a descriptive name appended to the daemon thread name for debugging
     */
    DataBinderCleaner(final DataBinder owner, final String name) {
        this.owner = owner;
        Thread.ofVirtual().name("DataBinderCleaner-" + name).start(this::cleanupLoop);
    }

    /**
     * Registers an {@link IBindable} receiver for automatic cleanup when it's garbage collected.
     *
     * @param receiver the container to monitor for garbage collection
     */
    void registerReceiver(final IBindable receiver) {
        UUID id = receiver.getID();
        PhantomReference<IBindable> phantomRef = new PhantomReference<>(receiver, this.referenceQueue);
        this.receiverMap.put(phantomRef, id);
    }

    /**
     * Registers an {@link IBindable} transmitter for automatic cleanup when it's garbage collected.
     *
     * @param container the container to monitor for garbage collection
     */
    void registerTransmitter(final IBindable container) {
        UUID id = container.getID();
        PhantomReference<IBindable> phantomRef = new PhantomReference<>(container, this.referenceQueue);
        this.transmitterMap.put(phantomRef, id);
    }

    /**
     * Main cleanup loop that runs in a background daemon thread. Continuously monitors for garbage collected objects
     * and cleans up their bindings.
     * <p>
     * Any {@link Throwable} thrown from the loop body is handled uniformly: the loop re-arms after a short backoff, up
     * to {@code RestartGuard.MAX_RESTARTS} times within a sliding window. If the cap is exceeded the owning
     * {@link DataBinder} is fail-stopped (see {@link DataBinder#failStop()}), the exception is logged, and the loop
     * exits. Shutdown is driven by the {@link #shutdownRequested} flag, polled via
     * {@link ReferenceQueue#remove(long)}; the loop does not rely on {@link Thread#interrupt()}.
     * </p>
     */
    private void cleanupLoop() {
        while (!this.shutdownRequested) {
            try {
                @SuppressWarnings("unchecked")
                PhantomReference<IBindable> phantomRef =
                        (PhantomReference<IBindable>) this.referenceQueue.remove(POLL_TIMEOUT_MS);
                if (phantomRef != null) {
                    processPhantomReference(phantomRef);
                }
                this.restartGuard.decay();
            }
            catch (Throwable t) {
                if (!handleFailure(t)) {
                    break;
                }
            }
        }
    }

    /**
     * Handles any {@link Throwable} thrown from the loop body. Re-arms after a short backoff if the restart cap has
     * not been exceeded; otherwise logs the failure, fail-stops the owning {@link DataBinder}, and signals exit.
     *
     * @param t the throwable thrown by the loop body
     * @return {@code true} to continue the loop; {@code false} to exit (shutdown or fail-stop)
     */
    private boolean handleFailure(final Throwable t) {
        if (this.shutdownRequested) {
            return false;
        }
        if (!this.restartGuard.allowRestart()) {
            System.err.println("Error in DataBinderCleaner thread: " + t.getMessage());
            failStopBestEffort();
            return false;
        }
        this.restartGuard.sleepBackoff();
        return true;
    }

    /**
     * Best-effort invocation of {@link DataBinder#failStop()}, swallowing any secondary exception. Intended for the
     * {@code catch (Error)} path where re-throwing or propagating secondary failures is undesirable.
     */
    private void failStopBestEffort() {
        try {
            this.owner.failStop();
        }
        catch (Exception ignore) {
            // best-effort under Error
        }
    }

    private void processPhantomReference(final PhantomReference<IBindable> phantomRef) {
        UUID transmitterID = this.transmitterMap.remove(phantomRef);
        if (transmitterID != null) {
            this.owner.cleanupTransmitter(transmitterID);
        }

        UUID receiverID = this.receiverMap.remove(phantomRef);
        if (receiverID != null) {
            this.owner.cleanupReceiver(receiverID);
        }

        phantomRef.clear();
    }

    /**
     * Gets the number of containers currently being monitored for garbage collection. Useful for testing and monitoring
     * purposes.
     *
     * @return the number of registered phantom references
     */
    int getMonitoredContainerCount() {
        return this.transmitterMap.size() + this.receiverMap.size();
    }

    /**
     * Requests the background daemon thread to exit its cleanup loop. Sets the {@code shutdownRequested} flag; the
     * loop notices within at most {@value #POLL_TIMEOUT_MS} ms via its {@link ReferenceQueue#remove(long)} timeout.
     * This
     * method does <em>not</em> call {@link Thread#interrupt()}, so any {@link InterruptedException} observed by the
     * loop is, by construction, accidental (and triggers the restart path). After shutdown, phantom references will no
     * longer be processed automatically; tests may use {@code TestDataBinder.drainOnce()} for manual processing.
     */
    void shutdown() {
        this.shutdownRequested = true;
    }

    /**
     * Restart-rate limiter for the cleanup loop. Caps the number of accidental-interrupt re-arms within a sliding
     * time window to prevent infinite respawn thrash. Mutated only by the single cleaner thread, so no synchronization
     * is required.
     */
    private static final class RestartGuard {

        private static final int MAX_RESTARTS = 5;
        private static final long RESTART_WINDOW_MS = 60_000L;
        private static final long BACKOFF_MS = 500L;

        private int count = 0;
        private long windowStartMs = 0L;

        /**
         * Records a restart attempt and enforces the cap within the current sliding window.
         *
         * @return {@code true} if the restart is allowed; {@code false} if the cap was exceeded within the window
         */
        boolean allowRestart() {
            long now = System.currentTimeMillis();
            if ((now - this.windowStartMs) > RESTART_WINDOW_MS) {
                this.windowStartMs = now;
                this.count = 1;
            }
            else {
                this.count++;
            }
            return this.count <= MAX_RESTARTS;
        }

        /**
         * Resets the counter if the sliding window has elapsed, so a transient burst of failures early on does not
         * permanently exhaust the cap.
         */
        void decay() {
            if ((this.count > 0) && ((System.currentTimeMillis() - this.windowStartMs) > RESTART_WINDOW_MS)) {
                this.count = 0;
            }
        }

        /**
         * Sleeps for the backoff duration. Interrupts during backoff are swallowed; the caller's loop condition
         * re-checks the shutdown flag on the next iteration.
         */
        void sleepBackoff() {
            try {
                Thread.sleep(BACKOFF_MS);
            }
            catch (InterruptedException e) {
                // swallowed; loop re-checks shutdown flag
            }
        }
    }
}
