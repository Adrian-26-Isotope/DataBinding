package adrian.os.java.databinding.demo;

import adrian.os.java.databinding.BaseDataContainer;
import adrian.os.java.databinding.DataFactory;
import adrian.os.java.databinding.DataSchema;
import adrian.os.java.databinding.FieldDefinition;


public class MasterDataV2 extends BaseDataContainer {

    public static final String NAME_FIELD = "name";
    public static final String TYPE_FIELD = "type";
    public static final String NOTES_FIELD = "notes";

    public static final DataSchema SCHEMA = new DataSchema(FieldDefinition.readWrite(NAME_FIELD),
            FieldDefinition.readWrite(TYPE_FIELD), FieldDefinition.readWrite(NOTES_FIELD));

    public MasterDataV2(final String name, final String type, final String notes) {
        super(SCHEMA);

        setFieldValue(NAME_FIELD, name);
        setFieldValue(TYPE_FIELD, type);
        setFieldValue(NOTES_FIELD, notes);
    }

    public String getName() {
        return getFieldValue(NAME_FIELD);
    }

    public void setName(final String name) {
        setFieldValue(NAME_FIELD, name);
    }

    public String getType() {
        return getFieldValue(TYPE_FIELD);
    }

    public void setType(final String type) {
        setFieldValue(TYPE_FIELD, type);
    }

    public String getNotes() {
        return getFieldValue(NOTES_FIELD);
    }

    public void setNotes(final String notes) {
        setFieldValue(NOTES_FIELD, notes);
    }

    /** Factory method for creating {@link SlaveData1V2} objects. */
    public SlaveData1V2 createSlaveData1() {
        return DataFactory.createFrom(this, SlaveData1V2.SCHEMA, SlaveData1V2::new);
    }

    /** Factory method for creating {@link SlaveData2V2} objects. */
    public SlaveData2V2 createSlaveData2() {
        return DataFactory.createFrom(this, SlaveData2V2.SCHEMA, SlaveData2V2::new);
    }

}
