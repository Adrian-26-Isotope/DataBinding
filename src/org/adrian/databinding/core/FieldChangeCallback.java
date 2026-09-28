package org.adrian.databinding.core;

/**
 * Functional interface for handling field change notifications in data binding.
 * Implementations of this interface are called when bound fields are modified.
 */
@FunctionalInterface
public interface FieldChangeCallback<T extends IBindable> {

    /**
     * Called when a field value changes in a bound object.
     *
     * @param event the field change event containing the receiver, field name,
     *            old and new values, and the update chain
     */
    void onFieldChange(FieldChangeEvent<T> event);
}
