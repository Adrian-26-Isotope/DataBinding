package org.adrian.databinding.demo;

import java.util.HashMap;
import java.util.Map;

import org.adrian.databinding.BaseDataContainer;
import org.adrian.databinding.DataFactory;
import org.adrian.databinding.DataSchema;
import org.adrian.databinding.FieldDefinition;


public class MasterData extends BaseDataContainer {

    public static final String NAME_FIELD = "name";
    public static final String TYPE_FIELD = "type";
    public static final String NOTES_FIELD = "notes";

    public static final DataSchema SCHEMA = new DataSchema(FieldDefinition.readWrite(NAME_FIELD, String.class),
            FieldDefinition.readWrite(TYPE_FIELD, String.class), FieldDefinition.readWrite(NOTES_FIELD, String.class));

    public MasterData(final String name, final String type, final String notes) {
        super(SCHEMA);

        Map<String, Object> inital = new HashMap<>();
        inital.put(NAME_FIELD, name);
        inital.put(TYPE_FIELD, type);
        inital.put(NOTES_FIELD, notes);
        initValues(inital);
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

    /** Factory method for creating {@link SlaveData1} objects. */
    public SlaveData1 createSlaveData1() {
        return DataFactory.createFrom(this, SlaveData1.SCHEMA, SlaveData1::new);
    }

    /** Factory method for creating {@link SlaveData2} objects. */
    public SlaveData2 createSlaveData2() {
        return DataFactory.createFrom(this, SlaveData2.SCHEMA, SlaveData2::new);
    }

}
