package org.adrian.databinding.data;

import org.adrian.databinding.BasicDataContainer;
import org.adrian.databinding.BasicSlaveContainer;
import org.adrian.databinding.DataSchema;
import org.adrian.databinding.FieldDefinition;

/**
 * Test fixture: slave container with mixed access — {@code name} and {@code type} read-only,
 * {@code notes} and {@code additionalInfo} read-write.
 */
public class TestSlaveData3 extends BasicSlaveContainer {

    public static final DataSchema SCHEMA =
            new DataSchema(FieldDefinition.readOnly(TestMasterData.NAME_FIELD, String.class),
                    FieldDefinition.readWrite(TestMasterData.NOTES_FIELD, String.class),
                    FieldDefinition.readOnly(TestMasterData.TYPE_FIELD, String.class),
                    FieldDefinition.readWrite(TestSlaveData1.ADDITIONAL_INFO_FIELD, String.class));

    public TestSlaveData3(final DataSchema schema, final BasicDataContainer master) {
        super(schema, master);
    }

    public String getName() {
        return getFieldValue(TestMasterData.NAME_FIELD, String.class);
    }

    public String getNotes() {
        return getFieldValue(TestMasterData.NOTES_FIELD, String.class);
    }

    public void setNotes(final String notes) {
        setFieldValue(TestMasterData.NOTES_FIELD, notes);
    }

    public String getType() {
        return getFieldValue(TestMasterData.TYPE_FIELD, String.class);
    }

    public String getAdditionalInfo() {
        return getFieldValue(TestSlaveData1.ADDITIONAL_INFO_FIELD, String.class);
    }

    public void setAdditionalInfo(final String additionalInfo) {
        setFieldValue(TestSlaveData1.ADDITIONAL_INFO_FIELD, additionalInfo);
    }
}
