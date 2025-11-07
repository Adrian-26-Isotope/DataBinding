package adrian.os.java.databinding.demo;

import adrian.os.java.databinding.BaseDataContainer;
import adrian.os.java.databinding.DataSchema;
import adrian.os.java.databinding.FieldDefinition;

/**
 * SlaveData2 that inherits only name and notes fields as read-only.
 */
public class SlaveData2 extends BaseDataContainer {

    public static final DataSchema SCHEMA = new DataSchema(FieldDefinition.readOnly(MasterData.NAME_FIELD),
            FieldDefinition.readOnly(MasterData.NOTES_FIELD));

    public SlaveData2(final DataSchema schema, final BaseDataContainer master) {
        super(schema, master);
    }


    public String getName() {
        return getFieldValue(MasterData.NAME_FIELD);
    }

    public String getNotes() {
        return getFieldValue(MasterData.NOTES_FIELD);
    }

    // Note: No setters since this is read-only
    // Any changes must come through data binding from the master
}
