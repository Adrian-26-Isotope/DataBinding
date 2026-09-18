package org.adrian.databinding.data;

import java.util.HashMap;
import java.util.Map;

import org.adrian.databinding.BasicDataContainer;
import org.adrian.databinding.BasicSlaveContainer;
import org.adrian.databinding.DataSchema;
import org.adrian.databinding.FieldDefinition;

/**
 * Test fixture: slave container with four read-write {@code String} fields — the
 * three master fields plus an {@code additionalInfo} field.
 */
public class TestSlaveData1 extends BasicSlaveContainer {

    public static final String ADDITIONAL_INFO_FIELD = "additionalInfo";

    public static final DataSchema SCHEMA =
            new DataSchema(FieldDefinition.readWrite(TestMasterData.NAME_FIELD, String.class),
                    FieldDefinition.readWrite(TestMasterData.TYPE_FIELD, String.class),
                    FieldDefinition.readWrite(TestMasterData.NOTES_FIELD, String.class),
                    FieldDefinition.readWrite(ADDITIONAL_INFO_FIELD, String.class));

    public TestSlaveData1(final DataSchema schema, final BasicDataContainer master) {
        Map<String, Object> inital = new HashMap<>();
        inital.put(ADDITIONAL_INFO_FIELD, "initial");
        super(schema, master, inital);
    }

    public String getName() {
        return getFieldValue(TestMasterData.NAME_FIELD, String.class);
    }

    public void setName(final String name) {
        setFieldValue(TestMasterData.NAME_FIELD, name);
    }

    public String getType() {
        return getFieldValue(TestMasterData.TYPE_FIELD, String.class);
    }

    public void setType(final String type) {
        setFieldValue(TestMasterData.TYPE_FIELD, type);
    }

    public String getNotes() {
        return getFieldValue(TestMasterData.NOTES_FIELD, String.class);
    }

    public void setNotes(final String notes) {
        setFieldValue(TestMasterData.NOTES_FIELD, notes);
    }

    public String getAdditionalInfo() {
        return getFieldValue(ADDITIONAL_INFO_FIELD, String.class);
    }

    public void setAdditionalInfo(final String additionalInfo) {
        setFieldValue(ADDITIONAL_INFO_FIELD, additionalInfo);
    }
}
