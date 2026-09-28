package org.adrian.databinding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.Map;

import org.adrian.databinding.core.DataSchema;
import org.adrian.databinding.core.FieldDefinition;
import org.adrian.databinding.core.TestDataBinder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for R2-M5: type validation on the internal propagation and
 * copy paths. Verifies that schema type mismatches between bound peers are
 * rejected at bind time, at copy time, and that consistent types propagate
 * successfully.
 */
class TypeValidationPropagationTest {

    private static final String FIELD = "value";

    @AfterEach
    void tearDown() {
        TestDataBinder.reset();
    }

    /**
     * Two containers with the same field name but different types must fail at
     * {@code bindTo} time, before any propagation occurs.
     */
    @Test
    void testTypeMismatchRejectedAtBindTime() {
        DataSchema stringSchema = new DataSchema(FieldDefinition.readWrite(FIELD, String.class));
        DataSchema integerSchema = new DataSchema(FieldDefinition.readWrite(FIELD, Integer.class));

        BasicMasterContainer stringContainer = new BasicMasterContainer(stringSchema) {};
        BasicMasterContainer integerContainer = new BasicMasterContainer(integerSchema) {};

        assertThrows(IllegalArgumentException.class, () -> stringContainer.bindTo(integerContainer, FIELD));
    }

    /**
     * A slave whose field type differs from the master's must fail during
     * {@code copyValuesFromMaster}, before binding is set up.
     */
    @Test
    void testTypeMismatchRejectedAtCopyTime() {
        DataSchema masterSchema = new DataSchema(FieldDefinition.readWrite(FIELD, String.class));
        DataSchema slaveSchema = new DataSchema(FieldDefinition.readWrite(FIELD, Integer.class));

        BasicMasterContainer master = new BasicMasterContainer(masterSchema) {
            {
                getFieldValues().get(FIELD).set("text");
                getFieldTimestamps().get(FIELD).set(1L);
            }
        };

        Map<String, Object> initial = new HashMap<>();
        assertThrows(IllegalArgumentException.class,
                () -> new BasicSlaveContainer(slaveSchema, master, initial) {});
    }

    /**
     * When both sides use the same field type, {@code bindTo} succeeds and
     * values propagate correctly in both directions.
     */
    @Test
    void testConsistentTypesPropagateSuccessfully() {
        DataSchema schema = new DataSchema(FieldDefinition.readWrite(FIELD, String.class));

        BasicMasterContainer master = new BasicMasterContainer(schema) {};
        BasicMasterContainer slave = new BasicMasterContainer(schema) {};

        master.bindTo(slave, FIELD);
        slave.bindTo(master, FIELD);

        master.setFieldValue(FIELD, "fromMaster");
        assertEquals("fromMaster", slave.getFieldValue(FIELD, String.class));

        slave.setFieldValue(FIELD, "fromSlave");
        assertEquals("fromSlave", master.getFieldValue(FIELD, String.class));
    }
}
