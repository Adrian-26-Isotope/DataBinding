package adrian.os.java.databinding.demo;

import adrian.os.java.databinding.BaseDataContainer;
import adrian.os.java.databinding.DataFactory;
import adrian.os.java.databinding.DataSchema;
import adrian.os.java.databinding.FieldDefinition;

/**
 * SlaveData1 that inherits all fields from MasterData with read-write access. And add a new field.
 */
public class SlaveData1V2 extends BaseDataContainer {

    public static final String ADDITIONAL_INFO_FIELD = "additionalInfo";

    public static final DataSchema SCHEMA = new DataSchema(FieldDefinition.readWrite(MasterDataV2.NAME_FIELD),
            FieldDefinition.readWrite(MasterDataV2.TYPE_FIELD), FieldDefinition.readWrite(MasterDataV2.NOTES_FIELD),
            FieldDefinition.readWrite(ADDITIONAL_INFO_FIELD));

    public SlaveData1V2(final BaseDataContainer master, final DataSchema schema) {
        super(schema, master);
        setFieldValue(ADDITIONAL_INFO_FIELD, "initial");
    }

    public String getName() {
        return getFieldValue(MasterDataV2.NAME_FIELD);
    }

    public void setName(final String name) {
        setFieldValue(MasterDataV2.NAME_FIELD, name);
    }

    public String getType() {
        return getFieldValue(MasterDataV2.TYPE_FIELD);
    }

    public void setType(final String type) {
        setFieldValue(MasterDataV2.TYPE_FIELD, type);
    }

    public String getNotes() {
        return getFieldValue(MasterDataV2.NOTES_FIELD);
    }

    public void setNotes(final String notes) {
        setFieldValue(MasterDataV2.NOTES_FIELD, notes);
    }

    public String getAdditionalInfo() {
        return getFieldValue(ADDITIONAL_INFO_FIELD);
    }

    public void setAdditionalInfo(final String additionalInfo) {
        setFieldValue(ADDITIONAL_INFO_FIELD, additionalInfo);
    }

    public SlaveData3V2 createSlaveData3() {
        return DataFactory.createFrom(this, SlaveData3V2.SCHEMA, SlaveData3V2::new);
    }
}
