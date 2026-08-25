package org.adrian.databinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Central registry for managing data binding relationships between objects. Provides thread-safe operations for
 * binding, unbinding, and notifying callbacks when field values change in bound objects.
 * <p>
 * {@code DataBinder} is a <em>multiton</em>: each named instance is an independent registry owning its own binding
 * indices and {@link DataBinderCleaner} daemon thread. Named instances are created lazily via {@link #get(String)}
 * and cached for reuse.
 * </p>
 * <p>
 * A <em>thread-local active instance</em> (see {@link #getActive()}, {@link #setActive(String)}) determines which
 * registry newly-constructed {@link BaseDataContainer}s bind into. {@link BaseDataContainer} captures the active
 * instance at construction time into a {@code final} field, so no runtime dependency on global state remains after
 * construction. The active instance defaults to {@code "default"} when no name has been set. {@link #setActive(String)}
 * returns a {@link Scope} ( {@code AutoCloseable}) that restores the previous active name when closed.
 * </p>
 */
public class DataBinder {

    /**
     * this is the name of the default data binder instance.
     */
    public static final String DEFAULT_INSTANCE = "default";
    private static final ThreadLocal<String> activeName = ThreadLocal.withInitial(() -> DEFAULT_INSTANCE);
    private static final ConcurrentMap<String, DataBinder> instances = new ConcurrentHashMap<>();

    // main data binding cache
    private final Map<UUID, Map<String, List<WeakFieldChangeCallback>>> transmitterBindings = new ConcurrentHashMap<>();
    // reverse index for efficient receiver cleanup
    private final Map<UUID, List<BindingReference>> receiverBindings = new ConcurrentHashMap<>();
    private final String name;
    private final DataBinderCleaner cleaner;
    private volatile boolean active = true;

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
     * Constructs a new {@code DataBinder} with the given name and starts its cleaner daemon thread.
     *
     * @param name the name of this instance; used for the daemon thread name and keyed in the instances map
     */
    private DataBinder(final String name) {
        this.name = name;
        this.cleaner = new DataBinderCleaner(this, name);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Multiton lifecycle
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Returns the named {@code DataBinder} instance, creating it lazily if it does not yet exist. Each named instance
     * is an independent registry with its own binding indices and cleaner daemon thread.
     *
     * @param name the name of the instance to retrieve or create
     * @return the (possibly newly-created) named instance
     */
    public static DataBinder get(final String name) {
        return instances.computeIfAbsent(name, DataBinder::new);
    }

    /**
     * Returns the active {@code DataBinder} instance for the current thread, creating it lazily if needed. The active
     * instance is thread-local and defaults to {@code "default"}. It is captured by {@link BaseDataContainer} at
     * construction time, so calling this method later returns whatever is currently active for the calling thread.
     *
     * @return the active instance for the current thread
     */
    public static DataBinder getActive() {
        return get(activeName.get());
    }

    /**
     * Sets the active {@code DataBinder} name for the current thread and returns a {@link Scope} that restores the
     * previous active name when closed. Subsequent calls to {@link #getActive()} (and thus {@link BaseDataContainer}
     * construction) on this thread will use the named instance until the returned scope is closed.
     * <p>
     * Typical usage with a try-with-resources statement:
     * </p>
     *
     * <pre>{@code
     * try (DataBinder.Scope scope = DataBinder.setActive("session-1")) {
     *     // containers created here bind into "session-1"
     * } // previous active (or "default") restored automatically
     * }</pre>
     *
     * <br>
     * Scopes may be nested: closing an inner scope restores the active name to the outer scope's name, not necessarily
     * "default".<br>
     *
     * @param name the name of the instance to make active for the current thread
     * @return a {@link Scope} that restores the previous active name when closed
     */
    public static Scope setActive(final String name) {
        String previous = activeName.get();
        activeName.set(name);
        return new Scope(previous);
    }

    /**
     * Removes a named {@code DataBinder} instance and shuts down its cleaner daemon thread. A new instance with the
     * same name can later be created via {@link #get(String)}.
     *
     * @param name the name of the instance to remove
     */
    public static void remove(final String name) {
        DataBinder removed = instances.remove(name);
        if (removed != null) {
            removed.shutdownInstance();
        }
    }

    /**
     * Shuts down this instance: marks it inactive, clears all binding indices, and stops the cleaner daemon. After
     * this call, {@link #bind} and {@link #update} throw {@link IllegalStateException}.
     */
    private void shutdownInstance() {
        this.active = false;
        this.transmitterBindings.clear();
        this.receiverBindings.clear();
        this.cleaner.shutdown();
    }

    /**
     * Fail-stop this instance without re-entering the cleaner. Invoked by {@link DataBinderCleaner} when its daemon
     * loop is dying (unrecoverable {@link Error} or restart cap exceeded). Marks the binder inactive and clears all
     * binding indices so subsequent {@link #bind}/{@link #update} calls fail fast. Does <em>not</em> call
     * {@link DataBinderCleaner#shutdown()} — the cleaner is the caller and is already exiting.
     */
    void failStop() {
        this.active = false;
        this.transmitterBindings.clear();
        this.receiverBindings.clear();
        instances.remove(this.name, this);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Core binding operations (instance methods)
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Binds a callback to a specific field on the specified transmitter object.
     *
     * @param transmitter the source object to monitor
     * @param fieldName the specific field name to monitor
     * @param receiver the instance receiving field updates from the transmitter
     * @param callback the callback to invoke when a field change gets triggered. <strong>IMPORTANT</strong> this
     *            callback must not capture the receiver instance. See {@link WeakFieldChangeCallback} for
     *            details.
     */
    public void bind(final IBindable transmitter, final String fieldName, final BaseDataContainer receiver,
            final FieldChangeCallback callback) {
        if ((transmitter == null) || (fieldName == null) || (receiver == null) || (callback == null)) {
            throw new IllegalArgumentException("no argument must not be null");
        }
        if (!this.active) {
            throw new IllegalStateException("DataBinder '" + this.name + "' has been removed");
        }
        WeakFieldChangeCallback weakCallback = new WeakFieldChangeCallback(receiver, callback);

        // add to main bindings cache
        this.transmitterBindings.compute(transmitter.getID(), (_, fieldsCallbacks) -> {
            Map<String, List<WeakFieldChangeCallback>> map =
                    (fieldsCallbacks != null) ? fieldsCallbacks : new ConcurrentHashMap<>();
            map.computeIfAbsent(fieldName, _ -> new CopyOnWriteArrayList<>()).add(weakCallback);
            return map;
        });

        // Add to receiver index for fast cleanup
        BindingReference bindingRef = new BindingReference(transmitter.getID(), fieldName, weakCallback);
        this.receiverBindings.computeIfAbsent(receiver.getID(), _ -> new CopyOnWriteArrayList<>()).add(bindingRef);

        // register for cleanup to avoid memory leaks
        this.cleaner.registerTransmitter(transmitter);
        this.cleaner.registerReceiver(receiver);
    }

    /**
     * Removes all bindings for a garbage collected {@link BaseDataContainer}. This method is called automatically by
     * the {@link DataBinderCleaner}.
     *
     * @param containerID the UUID of the garbage collected container
     */
    void cleanupTransmitter(final UUID containerID) {
        this.transmitterBindings.remove(containerID);
    }

    /**
     * Removes expired callbacks. This method scans all bindings and removes WeakFieldChangeCallback instances whose
     * receiver has been garbage collected.
     *
     * @param receiverId the UUID of the garbage collected receiver
     */
    void cleanupReceiver(final UUID receiverId) {
        List<BindingReference> receiverRefs = this.receiverBindings.remove(receiverId);
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
     * Notifies all bound callbacks about a field value change. Only triggers notifications if the old and new values
     * are actually different.
     *
     * @param source the source object where the change occurred
     * @param fieldName the name of the field that changed
     * @param oldValue the previous value of the field
     * @param newValue the new value of the field
     * @param chain the update chain to prevent infinite loops
     */
    public void update(final IBindable source, final String fieldName, final Object oldValue, final Object newValue,
            final UpdateChain chain) {
        if (!this.active) {
            throw new IllegalStateException("DataBinder '" + this.name + "' has been removed");
        }
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

    private void removeExpiredCallbacks(final UUID sourceId, final String fieldName,
            final List<WeakFieldChangeCallback> expiredCallbacks) {
        if (!expiredCallbacks.isEmpty()) {
            getSpecificCallbacks(sourceId, fieldName).removeAll(expiredCallbacks);
            cleanup(sourceId, fieldName);
        }
    }

    /** remove empty mappings from the cache */
    private void cleanup(final UUID sourceId, final String fieldName) {
        this.transmitterBindings.computeIfPresent(sourceId, (_, fieldsCallbacks) -> {
            fieldsCallbacks.computeIfPresent(fieldName, (_, callbacks) -> callbacks.isEmpty() ? null : callbacks);
            return fieldsCallbacks.isEmpty() ? null : fieldsCallbacks;
        });
    }

    private List<WeakFieldChangeCallback> getSpecificCallbacks(final UUID id, final String fieldName) {
        Map<String, List<WeakFieldChangeCallback>> fieldsCallbacks = this.transmitterBindings.get(id);
        if (fieldsCallbacks == null) {
            return new ArrayList<>();
        }

        List<WeakFieldChangeCallback> specificCallbacks = fieldsCallbacks.get(fieldName);
        if (specificCallbacks == null) {
            return new ArrayList<>();
        }

        return specificCallbacks;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Monitoring helpers (static, delegate to the active instance)
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Gets the number of registered transmitters on the active instance. Useful for testing and monitoring purposes.
     *
     * @return the number of registered transmitters
     */
    public static int getTransmitterCount() {
        return getActive().transmitterBindings.size();
    }

    /**
     * Gets the number of registered receivers on the active instance. Useful for testing and monitoring purposes.
     *
     * @return the number of registered receivers
     */
    public static int getReceiverCount() {
        return getActive().receiverBindings.size();
    }

    /**
     * Gets the number of containers currently being monitored for garbage collection on the active instance. Useful for
     * testing and monitoring purposes.
     *
     * @return the number of registered phantom references
     */
    public static int getMonitoredContainerCount() {
        return getActive().cleaner.getMonitoredContainerCount();
    }
}
