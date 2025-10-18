package adrian.os.java.databinding;

/**
 * Functional interface for handling field change notifications in data binding.
 * Implementations of this interface are called when bound fields are modified.
 */
@FunctionalInterface
public interface FieldChangeCallback {

    /**
     * Called when a field value changes in a bound object.
     *
     * @param fieldName the name of the field that changed
     * @param oldValue  the previous value of the field
     * @param newValue  the new value of the field
     * @param chain     the update chain to prevent infinite loops
     */
    void onFieldChange(String fieldName, Object oldValue, Object newValue, UpdateChain chain);
}
