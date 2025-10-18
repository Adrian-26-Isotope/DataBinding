package adrian.os.java.databinding;

import java.util.Arrays;
import java.util.List;

/**
 * Defines the schema for a data container with field definitions.
 */
public class DataSchema {

    private final List<FieldDefinition> fieldDefinitions;


    public DataSchema(final FieldDefinition... fieldDefinitions) {
        this.fieldDefinitions = Arrays.asList(fieldDefinitions);
    }

    public DataSchema(final List<FieldDefinition> fieldDefinitions) {
        this.fieldDefinitions = fieldDefinitions;
    }


    public List<FieldDefinition> getFieldDefinitions() {
        return this.fieldDefinitions;
    }

    public FieldDefinition getFieldDefinition(final String fieldName) {
        return this.fieldDefinitions.stream().filter(fd -> fd.getFieldName().equals(fieldName)).findFirst()
                .orElse(null);
    }


    public List<FieldDefinition> getReadableFields() {
        return this.fieldDefinitions.stream().filter(FieldDefinition::isReadable).toList();
    }

    public List<FieldDefinition> getWritableFields() {
        return this.fieldDefinitions.stream().filter(FieldDefinition::isWritable).toList();
    }
}
