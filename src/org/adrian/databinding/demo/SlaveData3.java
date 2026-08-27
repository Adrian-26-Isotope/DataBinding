package org.adrian.databinding.demo;

import org.adrian.databinding.BaseDataContainer;
import org.adrian.databinding.DataSchema;
import org.adrian.databinding.FieldDefinition;

public class SlaveData3 extends BaseDataContainer {

    public static final DataSchema SCHEMA = new DataSchema(
            FieldDefinition.readOnly(MasterData.NAME_FIELD, String.class),
            FieldDefinition.readWrite(MasterData.NOTES_FIELD, String.class),
            FieldDefinition.readOnly(MasterData.TYPE_FIELD, String.class),
            FieldDefinition.readWrite(SlaveData1.ADDITIONAL_INFO_FIELD, String.class));

    public SlaveData3(final DataSchema schema, final BaseDataContainer master) {
        super(schema, master);
    }

    public String getName() {
        return getFieldValue(MasterData.NAME_FIELD, String.class);
    }

    // No setName() - it's read-only

    public String getNotes() {
        return getFieldValue(MasterData.NOTES_FIELD, String.class);
    }

    public void setNotes(final String notes) {
        setFieldValue(MasterData.NOTES_FIELD, notes);
    }

    public String getType() {
        return getFieldValue(MasterData.TYPE_FIELD, String.class);
    }

    // No setType() - it's read-only

    public String getAdditionalInfo() {
        return getFieldValue(SlaveData1.ADDITIONAL_INFO_FIELD, String.class);
    }

    public void setAdditionalInfo(final String additionalInfo) {
        setFieldValue(SlaveData1.ADDITIONAL_INFO_FIELD, additionalInfo);
    }
}
