package adrian.os.java.databinding;

import java.util.Arrays;
import java.util.List;

/**
 * Defines the schema for a data container with field definitions.
 * Encapsulates the structure and access permissions for fields in a data
 * object.
 */
public class DataSchema {

    private final List<FieldDefinition> fieldDefinitions;

    /**
     * Creates a new DataSchema with the specified field definitions.
     *
     * @param fieldDefinitions variable number of field definitions
     */
    public DataSchema(final FieldDefinition... fieldDefinitions) {
        this.fieldDefinitions = Arrays.asList(fieldDefinitions);
    }

    /**
     * Creates a new DataSchema with the specified list of field definitions.
     *
     * @param fieldDefinitions list of field definitions
     */
    public DataSchema(final List<FieldDefinition> fieldDefinitions) {
        this.fieldDefinitions = fieldDefinitions;
    }

    /**
     * Gets all field definitions in this schema.
     *
     * @return list of all field definitions
     */
    public List<FieldDefinition> getFieldDefinitions() {
        return this.fieldDefinitions;
    }

    /**
     * Gets the field definition for a specific field name.
     *
     * @param fieldName the name of the field to find
     * @return the field definition, or null if not found
     */
    public FieldDefinition getFieldDefinition(final String fieldName) {
        return this.fieldDefinitions.stream().filter(fd -> fd.getFieldName().equals(fieldName)).findFirst()
                .orElse(null);
    }

    /**
     * Gets all field definitions that have read access.
     *
     * @return list of readable field definitions
     */
    public List<FieldDefinition> getReadableFields() {
        return this.fieldDefinitions.stream().filter(FieldDefinition::isReadable).toList();
    }

    /**
     * Gets all field definitions that have write access.
     *
     * @return list of writable field definitions
     */
    public List<FieldDefinition> getWritableFields() {
        return this.fieldDefinitions.stream().filter(FieldDefinition::isWritable).toList();
    }
}
