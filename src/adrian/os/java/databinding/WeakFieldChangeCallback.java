package adrian.os.java.databinding;

import java.lang.ref.WeakReference;
import java.util.Objects;

/**
 * A wrapper for {@link FieldChangeCallback}s that holds a weak reference to the
 * owner object.
 * This class prevents memory leaks by allowing the owner object to be garbage
 * collected even if the callback is still referenced in the {@link DataBinder}
 * cache.
 * <p>
 * <strong>IMPORTANT!</strong> Do not capture strong references to the owner
 * object in the callback implementation, as this would prevent garbage
 * collection and defeat the purpose of using a weak reference.
 * </p>
 *
 * <pre>
 *
 * // GOOD: Static method reference - no strong reference to owner
 * new WeakFieldChangeCallback(owner, OwnerClass::onFieldChange);
 *
 * // GOOD: Lambda function that doesn't capture the owner instance
 * new WeakFieldChangeCallback(ownerInstance, (owner, field, oldVal, newVal, chain) -> {
 *     owner.doSomething(field, oldVal, newVal, chain);
 * });
 *
 * // BAD: Instance method reference - creates memory leak, because it creates a strong reference to owner
 * new WeakFieldChangeCallback(owner, owner::onFieldChange);
 *
 * // BAD: Lambda that captures the 'ownerInstance' variable - creates memory leak
 * new WeakFieldChangeCallback(ownerInstance, (owner, field, oldVal, newVal, chain) -> {
 *     ownerInstance.doSomething(field, oldVal, newVal, chain); // captures 'ownerInstance' from outer scope
 * });
 *
 * </pre>
 * <p>
 * The callback will only be executed if the owner object is still alive (not
 * garbage collected).
 * </p>
 */
public class WeakFieldChangeCallback {

    private final WeakReference<BaseDataContainer> weakOwner;
    private final FieldChangeCallback callback;

    /**
     * Constructs a new WeakFieldChangeCallback with the specified owner and
     * callback.
     *
     * @param callbackOwner the object that owns this callback; held as a weak
     *            reference to prevent memory leaks
     * @param callback the actual callback implementation to execute when a
     *            field changes. <strong>IMPORTANT</strong> this
     *            callback must not capture the callbackOwner. See
     *            {@link WeakFieldChangeCallback} for details.
     * @throws NullPointerException if either parameter is null
     */
    public WeakFieldChangeCallback(final BaseDataContainer callbackOwner, final FieldChangeCallback callback) {
        this.weakOwner = new WeakReference<>(callbackOwner);
        this.callback = Objects.requireNonNull(callback);
    }

    /**
     * <p>
     * This method checks if the weakly referenced owner is still available before
     * executing its callback. If the owner has been garbage collected, the callback
     * will not be executed and false will be returned.
     * </p>
     *
     * @param fieldName the name of the field that changed
     * @param oldValue the previous value of the field (may be null)
     * @param newValue the new value of the field (may be null)
     * @param chain the update chain for tracking field change propagation
     * @return true if the callback was executed (owner still alive), false if the
     *         owner has been garbage collected and the callback was not executed
     */
    public boolean execute(final String fieldName, final Object oldValue, final Object newValue,
            final UpdateChain chain) {
        BaseDataContainer owner = this.weakOwner.get();
        if (owner != null) {
            this.callback.onFieldChange(owner, fieldName, oldValue, newValue, chain);
            return true;
        }
        return false;
    }

    /**
     * Checks if this callback is expired.
     * This is used during cleanup when a receiver has been garbage collected.
     *
     * @return true if this callback belongs to the specified receiver and the
     *         receiver is garbage collected
     */
    public boolean isExpired() {
        return this.weakOwner.get() == null;
    }
}
