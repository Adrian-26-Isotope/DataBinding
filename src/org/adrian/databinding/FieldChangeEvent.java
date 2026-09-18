package org.adrian.databinding;

/**
 * Immutable snapshot of a field change event, delivered to
 * {@link FieldChangeCallback} implementations.
 * <p>
 * Bundling the receiver, field name, old and new values, and the
 * {@link UpdateChain} into a single parameter object avoids positional-argument
 * confusion (e.g. swapping {@code oldValue} and {@code newValue}) and keeps the
 * {@link FieldChangeCallback#onFieldChange(FieldChangeEvent)} signature stable
 * as new fields are added.
 * </p>
 *
 * @param receiver the data container that shall react on the field change
 * @param fieldName the name of the field that changed
 * @param oldValue the previous value of the field (may be {@code null})
 * @param newValue the new value of the field (may be {@code null})
 * @param chain the update chain to prevent infinite loops
 */
public record FieldChangeEvent(
        BasicDataContainer receiver,
        String fieldName,
        Object oldValue,
        Object newValue,
        UpdateChain chain) {
}
