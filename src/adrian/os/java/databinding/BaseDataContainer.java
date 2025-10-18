package adrian.os.java.databinding;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Base class that handles all databinding logic.
 * Data classes can extend this instead of implementing all the databinding
 * logic themselves.
 */
public abstract class BaseDataContainer implements IBindable {

    private final UUID id = UUID.randomUUID();
    private final DataSchema schema;

    // Storage for field values, timestamps, and locks
    private final Map<String, AtomicReference<Object>> fieldValues = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> fieldTimestamps = new ConcurrentHashMap<>();
    private final Map<String, ReentrantReadWriteLock> fieldLocks = new ConcurrentHashMap<>();

    /**
     * Constructor for 'master' data instances.
     */
    protected BaseDataContainer(final DataSchema schema) {
        this.schema = schema;
        initFields();
    }

    /**
     * Constructor that automatically copies field values from a master container.
     */
    protected BaseDataContainer(final DataSchema schema, final BaseDataContainer master) {
        this.schema = schema;
        initFields();
        copyValuesFromMaster(master);
    }

    /**
     * Copy field values from master container for fields that exist in both schemas
     */
    private void copyValuesFromMaster(final BaseDataContainer master) {
        Map<String, Object> initialValues = new HashMap<>();
        for (FieldDefinition fieldDef : this.schema.getFieldDefinitions()) {
            String fieldName = fieldDef.getFieldName();
            if (fieldDef.isReadable()) {
                try {
                    Object value = master.getFieldValue(fieldName);
                    initialValues.put(fieldName, value);
                }
                catch (IllegalArgumentException e) {
                    // Field doesn't exist in master or not readable, skip it
                }
            }
        }
        initValues(initialValues);
    }

    @Override
    public UUID getID() {
        return this.id;
    }

    protected DataSchema getSchema() {
        return this.schema;
    }

    private void initFields() {
        for (FieldDefinition fieldDef : this.schema.getFieldDefinitions()) {
            String fieldName = fieldDef.getFieldName();
            this.fieldValues.put(fieldName, new AtomicReference<>());
            this.fieldTimestamps.put(fieldName, new AtomicLong(0L));
            this.fieldLocks.put(fieldName, new ReentrantReadWriteLock());
        }
    }

    /**
     * Initialize field values. This does not trigger any databinding updates.
     */
    protected void initValues(final Map<String, Object> initialValues) {
        for (Map.Entry<String, Object> entry : initialValues.entrySet()) {
            String fieldName = entry.getKey();
            Object value = entry.getValue();

            AtomicReference<Object> field = this.fieldValues.get(fieldName);
            if (field != null) {
                field.set(value);
            }
        }
    }

    /**
     * Generic getter for any field
     */
    @SuppressWarnings("unchecked")
    public <T> T getFieldValue(final String fieldName) {
        FieldDefinition fieldDef = this.schema.getFieldDefinition(fieldName);
        if ((fieldDef == null) || !fieldDef.isReadable()) {
            throw new IllegalArgumentException("Field '" + fieldName + "' is not readable");
        }

        AtomicReference<Object> fieldRef = this.fieldValues.get(fieldName);
        return fieldRef != null ? (T) fieldRef.get() : null;
    }

    /**
     * Generic setter for any field. Needs to be called by every specific public
     * setter!
     */
    protected void setFieldValue(final String fieldName, final Object value) {
        UpdateChain chain = new UpdateChain();
        chain.add(getID());
        setFieldValue(fieldName, value, chain);
    }

    /**
     * Internal setter with update chain
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
            DataBinder.update(this, fieldName, oldValue, newValue, chain);
        }
    }

    /**
     * Get all locks for the readable fields (for safe multi-field operations)
     */
    protected ReentrantReadWriteLock[] getReadableLocks() {
        return this.schema.getReadableFields().stream().map(fielDef -> this.fieldLocks.get(fielDef.getFieldName()))
                .toArray(ReentrantReadWriteLock[]::new);
    }

    /**
     * Get the lock for a specific field (used by DataFactory)
     */
    protected ReentrantReadWriteLock getFieldLock(final String fieldName) {
        return this.fieldLocks.get(fieldName);
    }

    /**
     * Bind this container to another bindable object
     */
    void bindTo(final IBindable bindable) {
        DataBinder.bind(bindable, this::updateField);
    }

    /**
     * Bind specific field to another bindable object
     */
    void bindTo(final IBindable bindable, final String fieldName) {
        DataBinder.bind(bindable, fieldName, this::updateField);
    }

    /**
     * Handle incoming field updates from other bound objects
     */
    private void updateField(final String fieldName, final Object oldValue, final Object newValue,
            final UpdateChain chain) {
        // Check if this object is already being updated in the current chain
        if (chain.contains(getID())) {
            return;
        }

        // Add this object to the update chain
        if (!chain.add(getID())) {
            return;
        }

        // Check if we have this field and if it's writable
        FieldDefinition fieldDef = this.schema.getFieldDefinition(fieldName);
        if (fieldDef != null) {
            setFieldValue(fieldName, newValue, chain);
        }
    }

    /**
     * Print all fields and their values to the console
     */
    public void printAllFields() {
        System.out.println("=== Data Container: " + getClass().getSimpleName() + " (ID: " + this.id + ") ===");

        for (FieldDefinition fieldDef : this.schema.getFieldDefinitions()) {
            String fieldName = fieldDef.getFieldName();

            if (fieldDef.isReadable()) {
                ReentrantReadWriteLock lock = this.fieldLocks.get(fieldName);
                lock.readLock().lock();
                try {
                    Object value = this.fieldValues.get(fieldName).get();
                    long timestamp = this.fieldTimestamps.get(fieldName).get();
                    System.out.println("  " + fieldName + ": " + value + " (timestamp: " + timestamp + ")");
                }
                finally {
                    lock.readLock().unlock();
                }
            }
            else {
                System.out.println("  " + fieldName + ": [not readable]");
            }
        }
        System.out.println();
    }


}
