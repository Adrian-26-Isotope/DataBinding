package org.adrian.databinding.demo;

import org.adrian.databinding.BaseDataContainer;
import org.adrian.databinding.DataSchema;
import org.adrian.databinding.FieldDefinition;

/**
 * SlaveData2 that inherits only name and notes fields as read-only.
 */
public class SlaveData2 extends BaseDataContainer {

    public static final DataSchema SCHEMA = new DataSchema(
            FieldDefinition.readOnly(MasterData.NAME_FIELD, String.class),
            FieldDefinition.readOnly(MasterData.NOTES_FIELD, String.class));

    public SlaveData2(final DataSchema schema, final BaseDataContainer master) {
        super(schema, master);
    }


    public String getName() {
        return getFieldValue(MasterData.NAME_FIELD, String.class);
    }

    public String getNotes() {
        return getFieldValue(MasterData.NOTES_FIELD, String.class);
    }

    // Note: No setters since this is read-only
    // Any changes must come through data binding from the master
}
