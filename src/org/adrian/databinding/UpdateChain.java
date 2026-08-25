package org.adrian.databinding;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Manages update chains to detect and prevent cycles in synchronization.
 * This class tracks the propagation of updates through a network of
 * synchronized objects
 * and provides monotonic sequence number for concurrent updates.
 */
public class UpdateChain {

    private static final AtomicLong TIMESTAMP_COUNTER = new AtomicLong();

    private final Set<UUID> updateChain = new HashSet<>();
    private final long timestamp = TIMESTAMP_COUNTER.incrementAndGet();

    /**
     * Gets the timestamp when this update chain was created.
     * Used for ordering concurrent updates to ensure consistent final state.
     *
     * @return the creation timestamp as a monotonically increasing sequence number
     */
    public long getTimestamp() {
        return this.timestamp;
    }

    /**
     * Checks if the given object is already being updated in the current chain.
     *
     * @param obj the object UUID to check
     * @return true if the object is already in the update chain (cycle detected)
     */
    public boolean contains(final UUID obj) {
        return this.updateChain.contains(obj);
    }

    /**
     * Adds an object to the current update chain.
     *
     * @param obj the object UUID to add
     * @return true if the object was added (not already present)
     */
    public boolean add(final UUID obj) {
        return this.updateChain.add(obj);
    }

    /**
     * Removes an object from the current update chain.
     *
     * @param obj the object UUID to remove
     * @return true if the object was removed successfully
     */
    public boolean remove(final UUID obj) {
        return this.updateChain.remove(obj);
    }

    /**
     * Clears the entire update chain for the current thread.
     */
    public void clear() {
        this.updateChain.clear();
    }

    /**
     * Gets the current chain size (useful for debugging).
     *
     * @return the number of objects in the current update chain
     */
    public int size() {
        return this.updateChain.size();
    }

}
