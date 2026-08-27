package org.adrian.databinding;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Base class that handles all data binding logic for data containers.
 * Provides thread-safe field access, automatic synchronization between bound
 * objects,
 * and timestamp-based conflict resolution for concurrent updates.
 */
public abstract class BaseDataContainer implements IBindable {

    private final UUID id = UUID.randomUUID();
    private final DataSchema schema;
    private final DataBinder binder;

    // Storage for field values, timestamps, and locks
    private final Map<String, AtomicReference<Object>> fieldValues = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> fieldTimestamps = new ConcurrentHashMap<>();
    private final Map<String, ReentrantReadWriteLock> fieldLocks = new ConcurrentHashMap<>();

    /**
     * Constructor for 'master' data instances that don't inherit from other containers. Captures the thread-local
     * active {@link DataBinder} instance at construction time.
     *
     * @param schema the schema defining the fields and their access permissions
     */
    protected BaseDataContainer(final DataSchema schema) {
        this(schema, DataBinder.getActive());
    }

    /**
     * Constructor for 'master' data instances that don't inherit from other containers, using an explicit
     * {@link DataBinder} instance.
     *
     * @param schema the schema defining the fields and their access permissions
     * @param binder the {@link DataBinder} instance this container registers its bindings with
     */
    protected BaseDataContainer(final DataSchema schema, final DataBinder binder) {
        this.schema = schema;
        this.binder = binder;
        initFields();
    }

    /**
     * Constructor that automatically copies field values from a master container.
     * Used for creating 'slave' objects that inherit values from a master. The slave inherits the master's
     * {@link DataBinder} instance, ensuring the master and slave share one binding registry.
     *
     * @param schema the schema defining the fields and their access permissions
     * @param master the master container to copy initial values from
     */
    protected BaseDataContainer(final DataSchema schema, final BaseDataContainer master) {
        this(schema, master.binder);
        copyValuesFromMaster(master);
    }

    /**
     * Copy field values from master container for fields that exist in both schemas.
     * <p>
     * Reads the master's raw stored references directly (not via
     * {@link #getFieldValue}) so that the slave stores the same raw value the
     * propagation path would deliver — {@code getFieldValue} wraps mutable
     * values in unmodifiable views or defensive copies, which must not be
     * stored as the field's backing value.
     */
    private void copyValuesFromMaster(final BaseDataContainer master) {
        for (FieldDefinition fieldDef : this.schema.getFieldDefinitions()) {
            String fieldName = fieldDef.getFieldName();
            if (!fieldDef.isReadable()) {
                continue;
            }
            FieldDefinition masterFieldDef = master.schema.getFieldDefinition(fieldName);
            if ((masterFieldDef == null) || !masterFieldDef.isReadable()) {
                continue;
            }
            AtomicReference<Object> masterField = master.fieldValues.get(fieldName);
            if (masterField == null) {
                continue;
            }
            Object value = masterField.get();
            AtomicReference<Object> field = this.fieldValues.get(fieldName);
            AtomicLong timestamp = this.fieldTimestamps.get(fieldName);
            AtomicLong masterTimestamp = master.fieldTimestamps.get(fieldName);
            if ((field != null) && (timestamp != null) && (masterTimestamp != null)) {
                field.set(value);
                timestamp.set(masterTimestamp.get());
            }
        }
    }

    /**
     * Gets the unique identifier for this data container.
     *
     * @return the unique UUID for this container
     */
    @Override
    public UUID getID() {
        return this.id;
    }

    /**
     * Gets the schema that defines this container's structure.
     *
     * @return the data schema
     */
    protected DataSchema getSchema() {
        return this.schema;
    }

    private void initFields() {
        for (FieldDefinition fieldDef : this.schema.getFieldDefinitions()) {
            String fieldName = fieldDef.getFieldName();
            this.fieldValues.put(fieldName, new AtomicReference<>());
            this.fieldTimestamps.put(fieldName, new AtomicLong(Long.MIN_VALUE));
            this.fieldLocks.put(fieldName, new ReentrantReadWriteLock());
        }
    }

    /**
     * Initialize field values without triggering any data binding updates.
     * Used during object construction to set initial values.
     *
     * @param initialValues map of field names to their initial values
     */
    protected void initValues(final Map<String, Object> initialValues) {
        if ((initialValues == null) || initialValues.isEmpty()) {
            return;
        }

        for (Map.Entry<String, Object> entry : initialValues.entrySet()) {
            String fieldName = entry.getKey();
            Object value = entry.getValue();

            FieldDefinition fieldDef = this.schema.getFieldDefinition(fieldName);
            if (fieldDef != null) {
                validateFieldType(fieldDef, value);
            }

            AtomicReference<Object> field = this.fieldValues.get(fieldName);
            if (field != null) {
                field.set(value);
            }
        }
    }

    /**
     * Type-checked getter for a field defined in the schema. Validates that the stored
     * value is assignable to the requested type before returning it.
     * <p>
     * <strong>Propagation contract:</strong> {@code setFieldValue} is the
     * <em>only</em> path that triggers binding propagation (creates an
     * {@link UpdateChain}, fires callbacks, updates timestamps). In-place
     * mutation of a value returned by this method bypasses the binding
     * contract entirely — no callbacks fire, no timestamps are updated, and
     * custom receivers are silently skipped. Always use {@code setFieldValue}
     * with a new value to change a field; never mutate a returned value in
     * place.
     * <p>
     * <strong>Mutable-value wrapping:</strong> to prevent in-place mutation,
     * mutable values are wrapped before being returned:
     * <ul>
     *   <li>JDK {@link List}, {@link Set}, {@link Map}, and other
     *       {@link Collection} types are returned as unmodifiable views
     *       (no copy cost, mutation blocked at the API boundary).</li>
     *   <li>Arrays are returned as defensive copies.</li>
     *   <li>Custom types implementing {@link Copyable} are returned as a copy
     *       via {@code copy()}.</li>
     *   <li>Immutable types (e.g. {@code String}, {@code Integer}, records) are
     *       returned as-is.</li>
     * </ul>
     * Requesting a concrete collection type (e.g. {@code ArrayList.class})
     * will fail with {@link ClassCastException} because the returned value is
     * an unmodifiable view; request the interface type ({@code List.class},
     * {@code Set.class}, {@code Map.class}) instead.
     *
     * @param <T> the expected type of the field value
     * @param fieldName the name of the field to retrieve
     * @param type the {@link Class} object for type {@code T}; must not be {@code null}
     * @return the current value of the field, cast to {@code T} (wrapped if mutable)
     * @throws IllegalArgumentException if the field is not readable or doesn't exist,
     *             or if {@code type} is {@code null}
     * @throws ClassCastException if the stored value is not an instance of {@code T}
     */
    protected <T> T getFieldValue(final String fieldName, final Class<T> type) {
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }
        FieldDefinition fieldDef = this.schema.getFieldDefinition(fieldName);
        if (fieldDef == null) {
            throw new IllegalArgumentException("Field '" + fieldName + "' not present");
        }
        if (!fieldDef.isReadable()) {
            throw new IllegalArgumentException("Field '" + fieldName + "' is not readable");
        }

        AtomicReference<Object> fieldRef = this.fieldValues.get(fieldName);
        Object rawValue = fieldRef != null ? fieldRef.get() : null;
        Object value = wrapIfMutable(rawValue);
        if ((value != null) && !type.isInstance(value)) {
            throw new ClassCastException("Field '" + fieldName + "' holds a " + value.getClass().getName() +
                    ", cannot be cast to " + type.getName());
        }
        return type.cast(value);
    }

    /**
     * Validates that the given value matches the field's declared type. Does nothing if
     * {@code value} is {@code null} (null is always accepted) or if the field type is
     * {@code Object.class}.
     *
     * @param fieldDef the field definition carrying the expected type
     * @param value the value to validate
     * @throws IllegalArgumentException if {@code value} is not {@code null} and not an
     *             instance of the field's declared type
     */
    private static void validateFieldType(final FieldDefinition fieldDef, final Object value) {
        if ((value != null) && !fieldDef.getType().isInstance(value)) {
            throw new IllegalArgumentException("Field '" + fieldDef.getFieldName() + "' expects " +
                    fieldDef.getType().getName() + " but got " + value.getClass().getName());
        }
    }

    /**
     * Wraps a raw field value to prevent in-place mutation by callers of
     * {@link #getFieldValue}. The wrapping strategy depends on the value's type:
     * <ol>
     *   <li>{@link Copyable} — returns {@code copy()} (checked first so that a
     *       custom type's explicit opt-in takes precedence over any collection
     *       interface it may also implement).</li>
     *   <li>{@link List} — unmodifiable view.</li>
     *   <li>{@link Set} — unmodifiable view.</li>
     *   <li>{@link Map} — unmodifiable view.</li>
     *   <li>Other {@link Collection} — unmodifiable view.</li>
     *   <li>Array — defensive copy (preserves component type).</li>
     *   <li>Everything else (immutable types) — returned as-is.</li>
     * </ol>
     * Returns {@code null} if the input is {@code null}.
     *
     * @param value the raw stored value
     * @return the wrapped value, or {@code null}
     */
    private static Object wrapIfMutable(final Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Copyable<?> copyable) {
            return copyable.copy();
        }
        if (value instanceof List<?> list) {
            return Collections.unmodifiableList(list);
        }
        if (value instanceof Set<?> set) {
            return Collections.unmodifiableSet(set);
        }
        if (value instanceof Map<?, ?> map) {
            return Collections.unmodifiableMap(map);
        }
        if (value instanceof Collection<?> collection) {
            return Collections.unmodifiableCollection(collection);
        }
        if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            Class<?> componentType = value.getClass().getComponentType();
            Object copy = Array.newInstance(componentType, length);
            System.arraycopy(value, 0, copy, 0, length);
            return copy;
        }
        return value;
    }

    /**
     * Generic setter for any field. Must be called by every specific public setter
     * to ensure proper data binding and synchronization.
     *
     * @param fieldName the name of the field to set
     * @param value the new value for the field
     */
    protected void setFieldValue(final String fieldName, final Object value) {
        FieldDefinition fieldDef = this.schema.getFieldDefinition(fieldName);
        if (fieldDef == null) {
            throw new IllegalArgumentException("Field '" + fieldName + "' not present.");
        }
        if (!fieldDef.isWritable()) {
            throw new IllegalArgumentException("Field '" + fieldName + "' is not writable.");
        }
        validateFieldType(fieldDef, value);

        UpdateChain chain = new UpdateChain();
        chain.add(getID());
        setFieldValue(fieldName, value, chain);
    }

    /**
     * Internal setter with update chain.<br>
     * This method is called by data binding mechanism. Field value must receive updates from master, even if
     * not writable by user.
     */
    private void setFieldValue(final String fieldName, final Object newValue, final UpdateChain chain) {
        FieldDefinition fieldDef = this.schema.getFieldDefinition(fieldName);
        if (fieldDef == null) {
            throw new IllegalArgumentException("Field '" + fieldName + "' not present.");
        }

        AtomicReference<Object> field = this.fieldValues.get(fieldName);
        AtomicLong timestamp = this.fieldTimestamps.get(fieldName);
        ReentrantReadWriteLock lock = this.fieldLocks.get(fieldName);

        if ((field == null) || (timestamp == null) || (lock == null)) {
            return;
        }

        Object oldValue;
        boolean update = false;

        lock.writeLock().lock();
        try {
            // Check if this update is newer than the current value
            long currentTimestamp = timestamp.get();
            if (chain.getTimestamp() <= currentTimestamp) {
                // This update is older, ignore it
                return;
            }
            timestamp.set(chain.getTimestamp());

            oldValue = field.getAndSet(newValue);
            update = true;
        }
        finally {
            lock.writeLock().unlock();
        }

        if (update) {
            this.binder.update(this, fieldName, oldValue, newValue, chain);
        }
    }

    /**
     * Get the lock for a specific field.
     *
     * @param fieldName the name of the field
     * @return the read-write lock for the specified field
     */
    ReentrantReadWriteLock getFieldLock(final String fieldName) {
        return this.fieldLocks.get(fieldName);
    }

    /**
     * Binds a specific field on another bindable object to this container, registering this container as the
     * receiver of the transmitter's field-change notifications.
     * <p>
     * This method is public for advanced use cases where the caller needs a binding topology that
     * {@link DataFactory#createFrom} cannot express. In normal usage, prefer
     * {@link DataFactory#createFrom} — it copies field values from the master and sets up
     * bidirectional bindings based on the slave's schema.
     * <p>
     * <strong>Risks of manual binding:</strong>
     * <ul>
     * <li><b>Topology responsibility.</b> The caller is responsible for producing a valid binding
     * topology. Self-loops, mismatched field names, or missing reverse bindings are not
     * detected.</li>
     * <li><b>No-capture constraint.</b> The callback used internally is a static method reference
     * that does not capture the receiver. See {@link WeakFieldChangeCallback} for why this
     * matters.</li>
     * </ul>
     *
     * @param bindable the transmitter (source) object whose field changes should notify this container
     * @param fieldName the field name to bind
     */
    public void bindTo(final IBindable bindable, final String fieldName) {
        this.binder.bind(bindable, fieldName, this, BaseDataContainer::onFieldChange);
    }

    /**
     * Handle incoming field updates from other bound objects
     */
    private static void onFieldChange(final BaseDataContainer receiver, final String fieldName, final Object oldValue,
            final Object newValue, final UpdateChain chain) {
        // Check if this object is already being updated in the current chain
        if (chain.contains(receiver.getID())) {
            return;
        }

        // Add this object to the update chain
        if (!chain.add(receiver.getID())) {
            return;
        }

        // Check if we have this field and if it's writable
        FieldDefinition fieldDef = receiver.getSchema().getFieldDefinition(fieldName);
        if (fieldDef != null) {
            receiver.setFieldValue(fieldName, newValue, chain);
        }
    }

    /**
     * Gets the timestamp of the last update to the specified field.
     *
     * @param fieldName the name of the field
     * @return the field's timestamp, or {@code 0L} if the field is not present
     */
    long getFieldTimestamp(final String fieldName) {
        final AtomicLong timestamp = this.fieldTimestamps.get(fieldName);
        return timestamp != null ? timestamp.get() : 0L;
    }

    @Override
    public String toString() {
        return DataContainerPrinter.format(this);
    }

}
