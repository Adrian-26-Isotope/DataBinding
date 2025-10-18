package adrian.os.java.databinding;

/**
 * Defines a field with its access permissions and provides factory methods
 * for creating common field access patterns.
 */
public class FieldDefinition {

    /**
     * Enumeration defining the access modes for fields.
     */
    public enum AccessMode {
        READ_ONLY,
        READ_WRITE
    }

    private final String fieldName;
    private final AccessMode accessMode;

    /**
     * Creates a new field definition with the specified name and access mode.
     *
     * @param fieldName  the name of the field
     * @param accessMode the access permissions for the field
     */
    public FieldDefinition(final String fieldName, final AccessMode accessMode) {
        this.fieldName = fieldName;
        this.accessMode = accessMode;
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
     * Factory method for creating a read-only field definition.
     *
     * @param fieldName the name of the field
     * @return a read-only field definition
     */
    public static FieldDefinition readOnly(final String fieldName) {
        return new FieldDefinition(fieldName, AccessMode.READ_ONLY);
    }

    /**
     * Factory method for creating a read-write field definition.
     *
     * @param fieldName the name of the field
     * @return a read-write field definition
     */
    public static FieldDefinition readWrite(final String fieldName) {
        return new FieldDefinition(fieldName, AccessMode.READ_WRITE);
    }
}
