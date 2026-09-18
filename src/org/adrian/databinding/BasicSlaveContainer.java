package org.adrian.databinding;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Convenience base class for 'slave' data containers that inherit field values
 * from a master container. The slave inherits the master's {@link DataBinder}
 * instance so both share one binding registry. Slave-only fields (not present
 * in the master's schema) are set from the slave's own initial values; shared
 * fields are copied from the master.
 */
public  class BasicSlaveContainer extends BasicDataContainer {

    /**
     * Constructor that automatically copies field values from a master container.
     * Used for creating 'slave' objects that inherit values from a master. The slave inherits the master's
     * {@link DataBinder} instance, ensuring the master and slave share one binding registry.
     *
     * @param schema the schema defining the fields and their access permissions
     * @param master the master container to copy initial values from
     */
    protected BasicSlaveContainer(final DataSchema schema, final BasicDataContainer master) {
        this(schema, master, new HashMap<>());
    }

    /**
     * Constructor that automatically copies field values from a master container,
     * then sets the slave's own initial values. The slave inherits the master's
     * {@link DataBinder} instance, ensuring the master and slave share one binding registry.
     * <p>
     * For fields present in both the slave's {@code initial} map and the master's
     * schema, the master's value takes precedence — {@code copyFromMaster} runs
     * after {@code initValues} and overwrites it.
     *
     * @param schema the schema defining the fields and their access permissions
     * @param master the master container to copy initial values from
     * @param initial the slave's own initial field values for fields not present in the master; may be empty
     */
    protected BasicSlaveContainer(final DataSchema schema, final BasicDataContainer master,
            final Map<String, Object> initial) {
        super(schema, master.getBinder(), initial);
        copyValuesFromMaster(master);
    }


    /**
     * Copy field values from master container for fields that exist in both schemas.
     * <p>
     * Reads the master's raw stored references directly (not via
     * {@code getFieldValue}) so that the slave stores the same raw value the
     * propagation path would deliver — {@code getFieldValue} wraps mutable
     * values in unmodifiable views or defensive copies, which must not be
     * stored as the field's backing value.
     *
     * @param master the master container to copy from
     */
    private void copyValuesFromMaster(final BasicDataContainer master) {
        for (FieldDefinition fieldDef : getSchema().getFieldDefinitions()) {
            String fieldName = fieldDef.getFieldName();
            if (!fieldDef.isReadable()) {
                continue;
            }
            FieldDefinition masterFieldDef = master.getSchema().getFieldDefinition(fieldName);
            if ((masterFieldDef == null) || !masterFieldDef.isReadable()) {
                continue;
            }
            AtomicReference<Object> masterField = master.getFieldValues().get(fieldName);
            if (masterField == null) {
                continue;
            }
            Object value = masterField.get();
            AtomicReference<Object> field = getFieldValues().get(fieldName);
            AtomicLong timestamp = getFieldTimestamps().get(fieldName);
            AtomicLong masterTimestamp = master.getFieldTimestamps().get(fieldName);
            if ((field != null) && (timestamp != null) && (masterTimestamp != null)) {
                field.set(value);
                timestamp.set(masterTimestamp.get());
            }
        }
    }

}
