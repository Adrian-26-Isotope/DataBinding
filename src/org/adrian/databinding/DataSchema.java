package org.adrian.databinding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Defines the schema for a data container with field definitions.
 * Encapsulates the structure and access permissions for fields in a data
 * object.
 */
public class DataSchema {

    private final Map<String, FieldDefinition> fieldDefinitionMap = new HashMap<>();

    /**
     * Creates a new DataSchema with the specified field definitions.
     *
     * @param fieldDefinitions variable number of field definitions
     */
    public DataSchema(final FieldDefinition... fieldDefinitions) {
        this(Arrays.asList(fieldDefinitions));
    }

    /**
     * Creates a new DataSchema with the specified list of field definitions.
     *
     * @param fieldDefinitions list of field definitions
     */
    public DataSchema(final List<FieldDefinition> fieldDefinitions) {
        for (FieldDefinition fd : fieldDefinitions) {
            String fieldName = fd.getFieldName();
            if (this.fieldDefinitionMap.containsKey(fieldName)) {
                throw new IllegalArgumentException("Duplicate field name: " + fieldName);
            }
            this.fieldDefinitionMap.put(fieldName, fd);
        }
    }

    /**
     * Gets all field definitions in this schema.
     *
     * @return unmodifiable list of all field definitions
     */
    public List<FieldDefinition> getFieldDefinitions() {
        return Collections.unmodifiableList(new ArrayList<>(this.fieldDefinitionMap.values()));
    }

    /**
     * Gets the field definition for a specific field name.
     *
     * @param fieldName the name of the field to find
     * @return the field definition, or null if not found
     */
    public FieldDefinition getFieldDefinition(final String fieldName) {
        return this.fieldDefinitionMap.get(fieldName);
    }

    /**
     * Gets all field definitions that have read access.
     *
     * @return list of readable field definitions
     */
    public List<FieldDefinition> getReadableFields() {
        return this.fieldDefinitionMap.values().stream().filter(FieldDefinition::isReadable).toList();
    }

    /**
     * Gets all field definitions that have write access.
     *
     * @return list of writable field definitions
     */
    public List<FieldDefinition> getWritableFields() {
        return this.fieldDefinitionMap.values().stream().filter(FieldDefinition::isWritable).toList();
    }
}
