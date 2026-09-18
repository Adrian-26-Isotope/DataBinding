package org.adrian.databinding.data;

import java.util.HashMap;
import java.util.Map;

import org.adrian.databinding.BasicMasterContainer;
import org.adrian.databinding.DataSchema;
import org.adrian.databinding.FieldDefinition;

/**
 * Test fixture: master container with three read-write {@code String} fields.
 * Mirrors the demo {@code MasterData} without carrying factory methods.
 */
public class TestMasterData extends BasicMasterContainer {

    public static final String NAME_FIELD = "name";
    public static final String TYPE_FIELD = "type";
    public static final String NOTES_FIELD = "notes";

    public static final DataSchema SCHEMA = new DataSchema(FieldDefinition.readWrite(NAME_FIELD, String.class),
            FieldDefinition.readWrite(TYPE_FIELD, String.class), FieldDefinition.readWrite(NOTES_FIELD, String.class));

    public TestMasterData(final String name, final String type, final String notes) {
        Map<String, Object> initial = new HashMap<>();
        initial.put(NAME_FIELD, name);
        initial.put(TYPE_FIELD, type);
        initial.put(NOTES_FIELD, notes);
        super(SCHEMA, initial);
    }

    public String getName() {
        return getFieldValue(NAME_FIELD, String.class);
    }

    public void setName(final String name) {
        setFieldValue(NAME_FIELD, name);
    }

    public String getType() {
        return getFieldValue(TYPE_FIELD, String.class);
    }

    public void setType(final String type) {
        setFieldValue(TYPE_FIELD, type);
    }

    public String getNotes() {
        return getFieldValue(NOTES_FIELD, String.class);
    }

    public void setNotes(final String notes) {
        setFieldValue(NOTES_FIELD, notes);
    }
}
