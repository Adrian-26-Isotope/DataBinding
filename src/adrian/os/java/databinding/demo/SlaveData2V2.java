package adrian.os.java.databinding.demo;

import adrian.os.java.databinding.BaseDataContainer;
import adrian.os.java.databinding.DataSchema;
import adrian.os.java.databinding.FieldDefinition;

/**
 * SlaveData2 that inherits only name and notes fields as read-only.
 */
public class SlaveData2V2 extends BaseDataContainer {

    public static final DataSchema SCHEMA = new DataSchema(FieldDefinition.readOnly(MasterDataV2.NAME_FIELD),
            FieldDefinition.readOnly(MasterDataV2.NOTES_FIELD));

    public SlaveData2V2(final BaseDataContainer master, final DataSchema schema) {
        super(schema, master);
    }


    public String getName() {
        return getFieldValue(MasterDataV2.NAME_FIELD);
    }

    public String getNotes() {
        return getFieldValue(MasterDataV2.NOTES_FIELD);
    }

    // Note: No setters since this is read-only
    // Any changes must come through data binding from the master
}
