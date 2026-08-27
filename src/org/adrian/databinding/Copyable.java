package org.adrian.databinding;

/**
 * Opt-in interface for mutable field-value types that should be defensively
 * copied when returned from {@link BaseDataContainer#getFieldValue}.
 * <p>
 * The data-binding framework's propagation contract relies on
 * {@code setFieldValue} being the only path that triggers an
 * {@link UpdateChain}. When {@code getFieldValue} returns a live mutable
 * reference, a caller can mutate it in place — bypassing propagation, skipping
 * registered callbacks, and (for read-only slaves) breaking access control.
 * <p>
 * For custom mutable
 * types, implement this interface so that {@code getFieldValue} returns a copy
 * instead of the live stored reference.
 * <p>
 * {@code copy()} is called on <strong>every</strong> {@code getFieldValue} call,
 * so it should be cheap. The copy is shallow by convention; if the object
 * contains nested mutable references, the implementor is responsible for
 * deep-copying them where necessary.
 * <p>
 * <strong>Recommendation:</strong> prefer immutable types (e.g. Java records)
 * for field values. Use {@code Copyable} only when mutability is unavoidable.
 *
 * @param <T> the type of the copy, normally the implementing class itself
 */
public interface Copyable<T> {

    /**
     * Creates and returns a copy of this object.
     * <p>
     * The returned copy should be independent of this instance — mutations to
     * the copy must not affect this object, and vice versa. A shallow copy is
     * acceptable when all fields are immutable or primitive; a deep copy is
     * required when the object holds references to other mutable objects.
     *
     * @return a new instance that is a copy of this object
     */
    T copy();
}
