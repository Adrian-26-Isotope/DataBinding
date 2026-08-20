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

    private volatile Thread cleanerThread;

    /**
     * Constructs a cleaner owned by the specified {@link DataBinder} and starts the background daemon thread.
     *
     * @param owner the {@link DataBinder} that owns this cleaner; used for cleanup callbacks
     * @param name  a descriptive name appended to the daemon thread name for debugging
     */
    DataBinderCleaner(final DataBinder owner, final String name) {
        this.owner = owner;
        this.cleanerThread = Thread.ofVirtual().name("DataBinderCleaner-" + name).start(this::cleanupLoop);
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
     */
    private void cleanupLoop() {
        while (true) {
            try {
                @SuppressWarnings("unchecked")
                PhantomReference<IBindable> phantomRef = (PhantomReference<IBindable>) this.referenceQueue.remove();
                processPhantomReference(phantomRef);
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            catch (Exception e) {
                System.err.println("Error in DataBinderCleaner thread: " + e.getMessage());
            }
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
     * Interrupts the background daemon thread, causing it to exit its cleanup loop. After shutdown, phantom references
     * will no longer be processed automatically; use {@link #drainOnce()} for manual processing.
     */
    void shutdown() {
        Thread thread = this.cleanerThread;
        if (thread != null) {
            thread.interrupt();
            this.cleanerThread = null;
        }
    }
}
