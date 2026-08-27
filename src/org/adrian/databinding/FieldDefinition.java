package org.adrian.databinding;

/**
 * Defines a field with its access permissions and provides factory methods
 * for creating common field access patterns.
 */
public class FieldDefinition {

    /**
     * Enumeration defining the access modes for fields.
     */
    @SuppressWarnings("javadoc")
    public enum AccessMode {
                            READ_ONLY,
                            READ_WRITE
    }

    private final String fieldName;
    private final AccessMode accessMode;
    private final Class<?> type;

    /**
     * Creates a new field definition with the specified name, access mode, and value type.
     * The type is used for write-time validation in {@code setFieldValue} / {@code initValues}
     * and for type-checked reads via {@code getFieldValue(name, type)}. Pass
     * {@code Object.class} to accept any value type.
     *
     * @param fieldName the name of the field
     * @param accessMode the access permissions for the field
     * @param type the expected runtime type of the field's value
     */
    public FieldDefinition(final String fieldName, final AccessMode accessMode, final Class<?> type) {
        this.fieldName = fieldName;
        this.accessMode = accessMode;
        this.type = type;
    }

    /**
     * Gets the name of this field.
     *
     * @return the field name
     */
    public String getFieldName() {
        return this.fieldName;
    }

    /**
     * Gets the access mode of this field.
     *
     * @return the access mode
     */
    public AccessMode getAccessMode() {
        return this.accessMode;
    }

    /**
     * Gets the expected runtime type of this field's value.
     *
     * @return the value type
     */
    public Class<?> getType() {
        return this.type;
    }

    /**
     * Checks if this field can be read.
     *
     * @return true if the field is readable
     */
    public boolean isReadable() {
        return (this.accessMode == AccessMode.READ_ONLY) || (this.accessMode == AccessMode.READ_WRITE);
    }

    /**
     * Checks if this field can be written to.
     *
     * @return true if the field is writable
     */
    public boolean isWritable() {
        return (this.accessMode == AccessMode.READ_WRITE);
    }

    /**
     * Factory method for creating a read-only field definition with a concrete value type.
     *
     * @param fieldName the name of the field
     * @param type the expected runtime type of the field's value
     * @return a read-only field definition
     */
    public static FieldDefinition readOnly(final String fieldName, final Class<?> type) {
        return new FieldDefinition(fieldName, AccessMode.READ_ONLY, type);
    }

    /**
     * Factory method for creating a read-write field definition with a concrete value type.
     *
     * @param fieldName the name of the field
     * @param type the expected runtime type of the field's value
     * @return a read-write field definition
     */
    public static FieldDefinition readWrite(final String fieldName, final Class<?> type) {
        return new FieldDefinition(fieldName, AccessMode.READ_WRITE, type);
    }
}
