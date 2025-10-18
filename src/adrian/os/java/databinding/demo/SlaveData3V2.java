package adrian.os.java.databinding.demo;

import adrian.os.java.databinding.BaseDataContainer;
import adrian.os.java.databinding.DataSchema;
import adrian.os.java.databinding.FieldDefinition;

public class SlaveData3V2 extends BaseDataContainer {

    public static final DataSchema SCHEMA = new DataSchema(FieldDefinition.readOnly(MasterDataV2.NAME_FIELD),
            FieldDefinition.readWrite(MasterDataV2.NOTES_FIELD), FieldDefinition.readOnly(MasterDataV2.TYPE_FIELD),
            FieldDefinition.readWrite(SlaveData1V2.ADDITIONAL_INFO_FIELD));

    public SlaveData3V2(final BaseDataContainer master, final DataSchema schema) {
        super(schema, master);
    }

    public String getName() {
        return getFieldValue(MasterDataV2.NAME_FIELD);
    }

    // No setName() - it's read-only

    public String getNotes() {
        return getFieldValue(MasterDataV2.NOTES_FIELD);
    }

    public void setNotes(final String notes) {
        setFieldValue(MasterDataV2.NOTES_FIELD, notes);
    }

    public String getType() {
        return getFieldValue(MasterDataV2.TYPE_FIELD);
    }

    // No setType() - it's read-only

    public String getAdditionalInfo() {
        return getFieldValue(SlaveData1V2.ADDITIONAL_INFO_FIELD);
    }

    public void setAdditionalInfo(final String additionalInfo) {
        setFieldValue(SlaveData1V2.ADDITIONAL_INFO_FIELD, additionalInfo);
    }
}
