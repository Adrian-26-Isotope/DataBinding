package adrian.os.java.databinding;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Handles automatic cleanup of {@link DataBinder} entries when {@link IBindable}
 * objects are garbage collected. Uses PhantomReference to detect when objects become
 * unreachable and removes their UUID mappings from the {@link DataBinder} cache.
 */
public class DataBinderCleaner {

    private static final DataBinderCleaner instance = new DataBinderCleaner();

    private final ReferenceQueue<IBindable> referenceQueue = new ReferenceQueue<>();
    private final ConcurrentMap<PhantomReference<IBindable>, UUID> transmitterMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<PhantomReference<IBindable>, UUID> receiverMap = new ConcurrentHashMap<>();

    private DataBinderCleaner() {
        Thread.ofVirtual().name("DataBinderCleaner").start(this::cleanupLoop);
    }

    /**
     * Registers a {@link IBindable} for automatic cleanup when it's garbage
     * collected.
     *
     * @param receiver the container to monitor for garbage collection
     */
    static void registerReceiver(final IBindable receiver) {
        UUID id = receiver.getID();
        PhantomReference<IBindable> phantomRef = new PhantomReference<>(receiver, instance.referenceQueue);
        instance.receiverMap.put(phantomRef, id);
    }

    /**
     * Registers a {@link IBindable} for automatic cleanup when it's garbage
     * collected.
     *
     * @param container the container to monitor for garbage collection
     */
    static void registerTransmitter(final IBindable container) {
        UUID id = container.getID();
        PhantomReference<IBindable> phantomRef = new PhantomReference<>(container, instance.referenceQueue);
        instance.transmitterMap.put(phantomRef, id);
    }

    /**
     * Main cleanup loop that runs in a background daemon thread.
     * Continuously monitors for garbage collected objects and cleans up their
     * bindings.
     */
    private void cleanupLoop() {
        while (true) {
            try {
                // Wait for a phantom reference to be enqueued (blocking call)
                @SuppressWarnings("unchecked")
                PhantomReference<IBindable> phantomRef = (PhantomReference<IBindable>) this.referenceQueue.remove();


                // Get the UUID associated with this phantom reference
                UUID transmitterID = this.transmitterMap.remove(phantomRef);
                if (transmitterID != null) {
                    // Remove all bindings for this container as a transmitter
                    DataBinder.cleanupTransmitter(transmitterID);
                }

                UUID receiverID = this.receiverMap.remove(phantomRef);
                if (receiverID != null) {
                    // Remove all expired callbacks
                    DataBinder.cleanupReceiver(receiverID);
                }

                // Clear the phantom reference
                phantomRef.clear();

            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            catch (Exception e) {
                // Log error but don't stop the cleanup thread
                System.err.println("Error in DataBinderCleaner thread: " + e.getMessage());
            }
        }
    }


    /**
     * Gets the number of containers currently being monitored for garbage
     * collection.
     * Useful for testing and monitoring purposes.
     *
     * @return the number of registered phantom references
     */
    public static int getMonitoredContainerCount() {
        return instance.transmitterMap.size() + instance.receiverMap.size();
    }
}
