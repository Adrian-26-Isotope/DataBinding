package adrian.os.java.databinding;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;


public class DataBinder {

    private static final DataBinder instance = new DataBinder();
    private final Map<UUID, Map<String, List<FieldChangeCallback>>> bindings = new ConcurrentHashMap<>();

    public static void bind(final IBindable source, final FieldChangeCallback callback) {
        bind(source, "*", callback);
    }

    public static void bind(final IBindable source, final String fieldName, final FieldChangeCallback callback) {
        instance.bindings.computeIfAbsent(source.getID(), k -> new ConcurrentHashMap<>())
                .computeIfAbsent(fieldName, k -> new CopyOnWriteArrayList<>()).add(callback);
    }

    public static void unbind(final IBindable source, final FieldChangeCallback callback) {
        unbind(source, "*", callback);
    }

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

    public static void unbindAll(final IBindable source) {
        instance.bindings.remove(source.getID());
    }

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

            // notify wildcard listeners
            List<FieldChangeCallback> wildcardCallbacks = fieldsCallbacks.get("*");
            if (wildcardCallbacks != null) {
                wildcardCallbacks.forEach(listener -> listener.onFieldChange(fieldName, oldValue, newValue, chain));
            }
        }
    }
}
