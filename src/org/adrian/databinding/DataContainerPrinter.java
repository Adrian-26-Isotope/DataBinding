package org.adrian.databinding;

import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Formats {@link BaseDataContainer} field state as a human-readable string for debugging.
 */
public final class DataContainerPrinter {

    private DataContainerPrinter() {}


    /**
     * Formats the container's fields as a multi-line string showing field names, values, and timestamps.
     * <p>
     * Each readable field is read under its per-field read lock. The snapshot is not atomic across fields;
     * a concurrent writer may interpose between individual field reads.
     *
     * @param container the container to format
     * @return a multi-line string representation of the container's field state
     */
    public static String format(final BaseDataContainer container) {
        final StringBuilder sb = new StringBuilder();
        sb.append("=== Data Container: ").append(container.getClass().getSimpleName()).append(" (ID: ")
                .append(container.getID()).append(") ===\n");

        for (FieldDefinition fieldDef : container.getSchema().getFieldDefinitions()) {
            final String fieldName = fieldDef.getFieldName();

            if (fieldDef.isReadable()) {
                final ReentrantReadWriteLock lock = container.getFieldLock(fieldName);
                lock.readLock().lock();
                try {
                    final Object value = container.getFieldValue(fieldName);
                    final long timestamp = container.getFieldTimestamp(fieldName);
                    sb.append("  ").append(fieldName).append(": ").append(value).append(" (timestamp: ")
                            .append(timestamp).append(")\n");
                }
                finally {
                    lock.readLock().unlock();
                }
            }
            else {
                sb.append("  ").append(fieldName).append(": [not readable]\n");
            }
        }
        return sb.toString();
    }
}
