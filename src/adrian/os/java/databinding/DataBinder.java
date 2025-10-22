package adrian.os.java.databinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Central registry for managing data binding relationships between objects.
 * Provides thread-safe operations for binding, unbinding, and notifying
 * callbacks
 * when field values change in bound objects.
 */
public class DataBinder {

    private static final DataBinder instance = new DataBinder();

    // main data binding cache
    private final Map<UUID, Map<String, List<WeakFieldChangeCallback>>> transmitterBindings = new ConcurrentHashMap<>();

    // reverse index for efficient receiver cleanup
    private final Map<UUID, List<BindingReference>> receiverBindings = new ConcurrentHashMap<>();

    /** Helper class to track where a receiver's callbacks are located */
    private static class BindingReference {

        private final UUID transmitterId;
        private final String transmitterFieldName;
        private final WeakFieldChangeCallback callback;

        private BindingReference(final UUID transmitterId, final String fieldName,
                final WeakFieldChangeCallback callback) {
            this.transmitterId = transmitterId;
            this.transmitterFieldName = fieldName;
            this.callback = callback;
        }
    }

    /**
     * Binds a callback to a specific field on the specified transmitter object.
     *
     * @param transmitter the source object to monitor
     * @param fieldName the specific field name to monitor
     * @param receiver the instance receiving field updates from the transmitter
     * @param callback the callback to invoke when a field change gets triggered. <strong>IMPORTANT</strong> this
     *            callback must not capture the receiver instance. See
     *            {@link WeakFieldChangeCallback} for details.
     */
    public static void bind(final IBindable transmitter, final String fieldName, final BaseDataContainer receiver,
            final FieldChangeCallback callback) {
        WeakFieldChangeCallback weakCallback = new WeakFieldChangeCallback(receiver, callback);

        // add to main bindings cache
        instance.transmitterBindings.computeIfAbsent(transmitter.getID(), k -> new ConcurrentHashMap<>())
                .computeIfAbsent(fieldName, k -> new CopyOnWriteArrayList<>()).add(weakCallback);

        // Add to receiver index for fast cleanup
        BindingReference bindingRef = new BindingReference(transmitter.getID(), fieldName, weakCallback);
        instance.receiverBindings.computeIfAbsent(receiver.getID(), k -> new CopyOnWriteArrayList<>()).add(bindingRef);

        // regsiter for cleanup to avoid memory leaks
        DataBinderCleaner.registerTransmitter(transmitter);
        DataBinderCleaner.registerReceiver(receiver);
    }


    /**
     * Removes all bindings for a garbage collected {@link BaseDataContainer}.
     * This method is called automatically by the GarbageCollectionCleanup system.
     *
     * @param containerID the UUID of the garbage collected container
     */
    static void cleanupTransmitter(final UUID containerID) {
        instance.transmitterBindings.remove(containerID);
    }

    /**
     * Removes expired callbacks.
     * This method scans all bindings and removes WeakFieldChangeCallback instances
     * whose receiver has been garbage collected.
     */
    static void cleanupReceiver(final UUID receiverId) {
        List<BindingReference> receiverRefs = instance.receiverBindings.remove(receiverId);
        if (receiverRefs != null) {
            for (BindingReference ref : receiverRefs) {
                List<WeakFieldChangeCallback> callbacks =
                        getSpecificCallbacks(ref.transmitterId, ref.transmitterFieldName);
                callbacks.remove(ref.callback);
                cleanup(ref.transmitterId, ref.transmitterFieldName);
            }
        }
    }


    /**
     * Notifies all bound callbacks about a field value change.
     * Only triggers notifications if the old and new values are actually different.
     *
     * @param source the source object where the change occurred
     * @param fieldName the name of the field that changed
     * @param oldValue the previous value of the field
     * @param newValue the new value of the field
     * @param chain the update chain to prevent infinite loops
     */
    public static void update(final IBindable source, final String fieldName, final Object oldValue,
            final Object newValue, final UpdateChain chain) {
        // IDEA: update to support events for more flexibility
        if ((oldValue == null) && (newValue == null)) {
            return;
        }
        if ((oldValue != null) && oldValue.equals(newValue)) {
            return;
        }

        List<WeakFieldChangeCallback> specificCallbacks = getSpecificCallbacks(source.getID(), fieldName);

        // notify specific field listeners
        List<WeakFieldChangeCallback> expiredCallbacks = new ArrayList<>();
        for (WeakFieldChangeCallback callback : specificCallbacks) {
            boolean executed = callback.execute(fieldName, oldValue, newValue, chain);
            if (!executed) {
                expiredCallbacks.add(callback);
            }
        }
        removeExpiredCallbacks(source.getID(), fieldName, expiredCallbacks);
    }


    private static void removeExpiredCallbacks(final UUID sourceId, final String fieldName,
            final List<WeakFieldChangeCallback> expiredCallbacks) {
        if (!expiredCallbacks.isEmpty()) {
            getSpecificCallbacks(sourceId, fieldName).removeAll(expiredCallbacks);
            cleanup(sourceId, fieldName);
        }
    }


    /** remove empty mappings from the cache */
    private static void cleanup(final UUID sourceId, final String fieldName) {
        Map<String, List<WeakFieldChangeCallback>> fieldsCallbacks = instance.transmitterBindings.get(sourceId);
        if (fieldsCallbacks == null) {
            return;
        }

        List<WeakFieldChangeCallback> specificCallbacks = fieldsCallbacks.get(fieldName);
        if (specificCallbacks == null) {
            return;
        }

        if (specificCallbacks.isEmpty()) {
            fieldsCallbacks.remove(fieldName);
            if (fieldsCallbacks.isEmpty()) {
                instance.transmitterBindings.remove(sourceId);
            }
        }
    }


    private static List<WeakFieldChangeCallback> getSpecificCallbacks(final UUID id, final String fieldName) {
        Map<String, List<WeakFieldChangeCallback>> fieldsCallbacks = instance.transmitterBindings.get(id);
        if (fieldsCallbacks == null) {
            return new ArrayList<>();
        }

        List<WeakFieldChangeCallback> specificCallbacks = fieldsCallbacks.get(fieldName);
        if (specificCallbacks == null) {
            return new ArrayList<>();
        }

        return specificCallbacks;
    }


    /**
     * Gets the number of registered transmitters.
     * Useful for testing and monitoring purposes.
     *
     * @return the number of registered phantom references
     */
    public static int getTransmitterCount() {
        return instance.transmitterBindings.size();
    }


    /**
     * Gets the number of registered receivers.
     * Useful for testing and monitoring purposes.
     *
     * @return the number of registered phantom references
     */
    public static int getReceiverCount() {
        return instance.receiverBindings.size();
    }


}
