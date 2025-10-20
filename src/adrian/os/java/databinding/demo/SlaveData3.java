package adrian.os.java.databinding.demo;

import adrian.os.java.databinding.BaseDataContainer;
import adrian.os.java.databinding.DataSchema;
import adrian.os.java.databinding.FieldDefinition;

public class SlaveData3 extends BaseDataContainer {

    public static final DataSchema SCHEMA = new DataSchema(FieldDefinition.readOnly(MasterData.NAME_FIELD),
            FieldDefinition.readWrite(MasterData.NOTES_FIELD), FieldDefinition.readOnly(MasterData.TYPE_FIELD),
            FieldDefinition.readWrite(SlaveData1.ADDITIONAL_INFO_FIELD));

    public SlaveData3(final BaseDataContainer master, final DataSchema schema) {
        super(schema, master);
    }

    public String getName() {
        return getFieldValue(MasterData.NAME_FIELD);
    }

    // No setName() - it's read-only

    public String getNotes() {
        return getFieldValue(MasterData.NOTES_FIELD);
    }

    public void setNotes(final String notes) {
        setFieldValue(MasterData.NOTES_FIELD, notes);
    }

    public String getType() {
        return getFieldValue(MasterData.TYPE_FIELD);
    }

    // No setType() - it's read-only

    public String getAdditionalInfo() {
        return getFieldValue(SlaveData1.ADDITIONAL_INFO_FIELD);
    }

    public void setAdditionalInfo(final String additionalInfo) {
        setFieldValue(SlaveData1.ADDITIONAL_INFO_FIELD, additionalInfo);
    }
}
