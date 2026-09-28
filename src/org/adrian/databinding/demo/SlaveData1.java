package org.adrian.databinding.demo;

import java.util.HashMap;
import java.util.Map;

import org.adrian.databinding.BasicDataContainer;
import org.adrian.databinding.BasicSlaveContainer;
import org.adrian.databinding.DataFactory;
import org.adrian.databinding.core.DataSchema;
import org.adrian.databinding.core.FieldDefinition;

/**
 * SlaveData1 that inherits all fields from MasterData with read-write access. And add a new field.
 */
public class SlaveData1 extends BasicSlaveContainer {

    public static final String ADDITIONAL_INFO_FIELD = "additionalInfo";

    public static final DataSchema SCHEMA =
            new DataSchema(FieldDefinition.readWrite(MasterData.NAME_FIELD, String.class),
                    FieldDefinition.readWrite(MasterData.TYPE_FIELD, String.class),
                    FieldDefinition.readWrite(MasterData.NOTES_FIELD, String.class),
                    FieldDefinition.readWrite(ADDITIONAL_INFO_FIELD, String.class));

    public SlaveData1(final DataSchema schema, final BasicDataContainer master) {
        Map<String, Object> inital = new HashMap<>();
        inital.put(ADDITIONAL_INFO_FIELD, "initial");
        super(schema, master, inital);
    }

    public String getName() {
        return getFieldValue(MasterData.NAME_FIELD, String.class);
    }

    public void setName(final String name) {
        setFieldValue(MasterData.NAME_FIELD, name);
    }

    public String getType() {
        return getFieldValue(MasterData.TYPE_FIELD, String.class);
    }

    public void setType(final String type) {
        setFieldValue(MasterData.TYPE_FIELD, type);
    }

    public String getNotes() {
        return getFieldValue(MasterData.NOTES_FIELD, String.class);
    }

    public void setNotes(final String notes) {
        setFieldValue(MasterData.NOTES_FIELD, notes);
    }

    public String getAdditionalInfo() {
        return getFieldValue(ADDITIONAL_INFO_FIELD, String.class);
    }

    public void setAdditionalInfo(final String additionalInfo) {
        setFieldValue(ADDITIONAL_INFO_FIELD, additionalInfo);
    }

    public SlaveData3 createSlaveData3() {
        return DataFactory.createFrom(SlaveData3.SCHEMA, this, SlaveData3::new);
    }
}
