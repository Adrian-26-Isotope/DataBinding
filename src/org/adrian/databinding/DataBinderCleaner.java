package org.adrian.databinding;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Handles automatic cleanup of {@link DataBinder} entries when {@link IBindable} objects are garbage collected. Uses
 * {@link PhantomReference} to detect when objects become unreachable and removes their UUID mappings from the owning
 * {@link DataBinder} cache.
 * <p>
 * This is a <em>shared singleton</em>: one instance backs all {@link DataBinder} instances in the JVM. A single
 * platform daemon thread blocks on a shared {@link ReferenceQueue}, so the number of cleaner threads is constant
 * regardless of how many named {@link DataBinder} instances exist.
 * </p>
 */
class DataBinderCleaner {

    private static final System.Logger LOGGER = System.getLogger(DataBinderCleaner.class.getName());

    private static final DataBinderCleaner INSTANCE = new DataBinderCleaner();

    /**
     * Associates a {@link PhantomReference} with the information needed to clean up the corresponding binding entries
     * in the owning {@link DataBinder}.
     *
     * @param owner the {@link DataBinder} that owns the binding
     * @param id the UUID of the garbage-collected container
     * @param transmitter {@code true} if this entry is a transmitter registration; {@code false} for a receiver
     */
    private record CleanupEntry(DataBinder owner, UUID id, boolean transmitter) {}

    private final ReferenceQueue<IBindable> referenceQueue = new ReferenceQueue<>();
    private final ConcurrentMap<PhantomReference<IBindable>, CleanupEntry> registry = new ConcurrentHashMap<>();

    /**
     * @return the shared singleton {@link DataBinderCleaner} instance
     */
    static DataBinderCleaner getInstance() {
        return INSTANCE;
    }

    /**
     * Constructs the shared cleaner and starts the background platform daemon thread. Private to enforce singleton
     * access via {@link #getInstance()}.
     */
    private DataBinderCleaner() {
        Thread.ofPlatform().daemon().name("DataBinderCleaner").start(this::cleanupLoop);
    }

    /**
     * Registers an {@link IBindable} receiver for automatic cleanup when it's garbage collected.
     *
     * @param owner the {@link DataBinder} that owns this binding
     * @param receiver the container to monitor for garbage collection
     */
    void registerReceiver(final DataBinder owner, final IBindable receiver) {
        UUID id = receiver.getId();
        PhantomReference<IBindable> phantomRef = new PhantomReference<>(receiver, this.referenceQueue);
        this.registry.put(phantomRef, new CleanupEntry(owner, id, false));
    }

    /**
     * Registers an {@link IBindable} transmitter for automatic cleanup when it's garbage collected.
     *
     * @param owner the {@link DataBinder} that owns this binding
     * @param transmitter the container to monitor for garbage collection
     */
    void registerTransmitter(final DataBinder owner, final IBindable transmitter) {
        UUID id = transmitter.getId();
        PhantomReference<IBindable> phantomRef = new PhantomReference<>(transmitter, this.referenceQueue);
        this.registry.put(phantomRef, new CleanupEntry(owner, id, true));
    }

    /**
     * Main cleanup loop that runs in a background daemon thread. Continuously monitors for garbage collected objects
     * and cleans up their bindings.
     * <p>
     * The loop blocks on the shared {@link ReferenceQueue} via {@link ReferenceQueue#remove()}, waking only when a
     * phantom reference is enqueued. Any {@link Throwable} thrown from the loop body is logged and the loop continues
     * after a short backoff — the shared cleaner never exits (except on JVM shutdown) so that all {@link DataBinder}
     * instances retain cleanup coverage.
     * </p>
     */
    private void cleanupLoop() {
        while (true) {
            try {
                @SuppressWarnings("unchecked")
                PhantomReference<IBindable> phantomRef = (PhantomReference<IBindable>) this.referenceQueue.remove();
                if (phantomRef != null) {
                    processReference(phantomRef);
                }
            }
            catch (InterruptedException e) {
                // spurious wake-up; continue
            }
            catch (Throwable t) {
                LOGGER.log(System.Logger.Level.ERROR, "Error in DataBinderCleaner thread: " + t.getMessage());
                try {
                    Thread.sleep(500L);
                }
                catch (InterruptedException ie) {
                    // interrupted during backoff; continue
                }
            }
        }
    }

    /**
     * Processes a single enqueued phantom reference by looking up its {@link CleanupEntry} and dispatching cleanup to
     * the owning {@link DataBinder}.
     *
     * @param phantomRef the enqueued phantom reference to process
     */
    private void processReference(final PhantomReference<IBindable> phantomRef) {
        CleanupEntry entry = this.registry.remove(phantomRef);
        if (entry != null) {
            if (entry.transmitter()) {
                entry.owner().cleanupTransmitter(entry.id());
            }
            else {
                entry.owner().cleanupReceiver(entry.id());
            }
        }
        phantomRef.clear();
    }

    /**
     * Removes all registry entries for the specified owning {@link DataBinder}. Used during {@link DataBinder} shutdown
     * and test reset to ensure no stale phantom references remain for the given owner.
     *
     * @param owner the {@link DataBinder} whose entries should be removed
     */
    void clearFor(final DataBinder owner) {
        this.registry.entrySet().removeIf(entry -> entry.getValue().owner() == owner);
    }

    /**
     * Gets the number of phantom references currently registered for the specified owning {@link DataBinder}. Useful
     * for testing and monitoring purposes.
     *
     * @param owner the {@link DataBinder} whose monitored count is requested
     * @return the number of registered phantom references for the given owner
     */
    long getMonitoredCountFor(final DataBinder owner) {
        return this.registry.values().stream().filter(entry -> entry.owner() == owner).count();
    }

    /**
     * Drains all currently-enqueued phantom references synchronously, processing each one the same way the background
     * daemon does. Allows tests to flush cleanup without relying on the daemon thread's latency.
     *
     * @return the number of phantom references processed
     */
    @SuppressWarnings("unchecked")
    int drainQueue() {
        int count = 0;
        PhantomReference<IBindable> phantomRef;
        while ((phantomRef = (PhantomReference<IBindable>) this.referenceQueue.poll()) != null) {
            processReference(phantomRef);
            count++;
        }
        return count;
    }
}
