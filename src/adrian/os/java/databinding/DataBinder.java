package adrian.os.java.databinding;

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
    private final Map<UUID, Map<String, List<FieldChangeCallback>>> bindings = new ConcurrentHashMap<>();


    /**
     * Binds a callback to a specific field on the specified source object.
     *
     * @param source the source object to bind to
     * @param fieldName the specific field name to monitor
     * @param callback the callback to invoke when the field changes
     */
    public static void bind(final IBindable source, final String fieldName, final FieldChangeCallback callback) {
        instance.bindings.computeIfAbsent(source.getID(), k -> new ConcurrentHashMap<>())
                .computeIfAbsent(fieldName, k -> new CopyOnWriteArrayList<>()).add(callback);
    }

    /**
     * Removes a callback from a specific field binding on the specified source
     * object.
     *
     * @param source the source object to unbind from
     * @param fieldName the specific field name to stop monitoring
     * @param callback the callback to remove
     */
    public static void unbind(final IBindable source, final String fieldName, final FieldChangeCallback callback) {
        Map<String, List<FieldChangeCallback>> fieldsCallbacks = instance.bindings.get(source.getID());
        if (fieldsCallbacks != null) {
            List<FieldChangeCallback> callbacks = fieldsCallbacks.get(fieldName);
            if (callbacks != null) {
                callbacks.remove(callback);
                // Clean up empty lists and maps
                if (callbacks.isEmpty()) {
                    fieldsCallbacks.remove(fieldName);
                    cleanupBinding(source, fieldsCallbacks);
                }
            }
        }
    }

    /**
     * Removes all bindings for the specified source object.
     *
     * @param source the source object to completely unbind
     */
    public static void unbindAll(final IBindable source) {
        instance.bindings.remove(source.getID());
    }

    /**
     * Removes all bindings for a specific field on the specified source object.
     *
     * @param source the source object to unbind from
     * @param fieldName the specific field name to unbind
     */
    public static void unbindAll(final IBindable source, final String fieldName) {
        Map<String, List<FieldChangeCallback>> fieldsCallbacks = instance.bindings.get(source.getID());
        if (fieldsCallbacks != null) {
            fieldsCallbacks.remove(fieldName);
            cleanupBinding(source, fieldsCallbacks);
        }
    }

    private static void cleanupBinding(final IBindable source,
            final Map<String, List<FieldChangeCallback>> fieldsCallbacks) {
        if (fieldsCallbacks.isEmpty()) {
            instance.bindings.remove(source.getID());
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
        else if ((oldValue != null) && oldValue.equals(newValue)) {
            return;
        }

        Map<String, List<FieldChangeCallback>> fieldsCallbacks = instance.bindings.get(source.getID());
        if (fieldsCallbacks != null) {

            // notify specific field listeners
            List<FieldChangeCallback> specificCallbacks = fieldsCallbacks.get(fieldName);
            if (specificCallbacks != null) {
                specificCallbacks.forEach(cb -> cb.onFieldChange(fieldName, oldValue, newValue, chain));
            }
        }
    }
}
