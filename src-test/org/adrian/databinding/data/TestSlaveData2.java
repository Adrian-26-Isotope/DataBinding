package org.adrian.databinding.data;

import org.adrian.databinding.BasicDataContainer;
import org.adrian.databinding.BasicSlaveContainer;
import org.adrian.databinding.DataSchema;
import org.adrian.databinding.FieldDefinition;

/**
 * Test fixture: read-only slave container with two fields — {@code name} and {@code notes}.
 */
public class TestSlaveData2 extends BasicSlaveContainer {

    public static final DataSchema SCHEMA =
            new DataSchema(FieldDefinition.readOnly(TestMasterData.NAME_FIELD, String.class),
                    FieldDefinition.readOnly(TestMasterData.NOTES_FIELD, String.class));

    public TestSlaveData2(final DataSchema schema, final BasicDataContainer master) {
        super(schema, master);
    }

    public String getName() {
        return getFieldValue(TestMasterData.NAME_FIELD, String.class);
    }

    public String getNotes() {
        return getFieldValue(TestMasterData.NOTES_FIELD, String.class);
    }
}
