package adrian.os.java.databinding;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Factory for creating data objects with proper initialization and binding.
 */
public class DataFactory {

    private DataFactory() {
        // Prevent instantiation
    }

    /**
     * Create a new data object from a master data source using the specified
     * schema.
     * This method handles safe copying of values and automatic binding setup.
     */
    public static <T extends BaseDataContainer> T createFrom(final BaseDataContainer master, final DataSchema schema,
            final DataObjectBuilder<T> builder) {

        // Get locks for all readable fields in the master that we need
        List<FieldDefinition> readableFields = schema.getReadableFields();
        ReentrantReadWriteLock[] masterLocks = readableFields.stream()
                .map(fieldDef -> master.getFieldLock(fieldDef.getFieldName()))
                .filter(Objects::nonNull).toArray(ReentrantReadWriteLock[]::new);

        List<Lock> acquiredLocks = null;
        try {
            acquiredLocks = MultiLockManager.lockAll(MultiLockManager.LockType.READ, masterLocks);

            // Create the new data object with current values from master
            T newDataObject = builder.build(master, schema);

            // Set up bidirectional binding
            setupBinding(master, newDataObject);

            return newDataObject;
        } finally {
            MultiLockManager.unlockAll(acquiredLocks);
        }
    }

    /**
     * Set up bidirectional binding between master and slave based on their schemas.
     */
    private static void setupBinding(final BaseDataContainer master, final BaseDataContainer slave) {
        // Bind slave to master only for readable fields in slave.
        List<FieldDefinition> readableFields = slave.getSchema().getReadableFields();
        for (FieldDefinition def : readableFields) {
            slave.bindTo(master, def.getFieldName());
        }

        // Bind master to slave only for writable fields in slave.
        List<FieldDefinition> writableFields = slave.getSchema().getWritableFields();
        for (FieldDefinition def : writableFields) {
            master.bindTo(slave, def.getFieldName());
        }
    }

}
