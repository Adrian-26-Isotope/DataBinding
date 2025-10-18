package adrian.os.java.databinding;

/**
 * Defines a field with its access permissions.
 */
public class FieldDefinition {

    public enum AccessMode {
                            READ_ONLY,
                            READ_WRITE
    }

    private final String fieldName;
    private final AccessMode accessMode;

    public FieldDefinition(final String fieldName, final AccessMode accessMode) {
        this.fieldName = fieldName;
        this.accessMode = accessMode;
    }

    public String getFieldName() {
        return this.fieldName;
    }

    public AccessMode getAccessMode() {
        return this.accessMode;
    }

    public boolean isReadable() {
        return (this.accessMode == AccessMode.READ_ONLY) || (this.accessMode == AccessMode.READ_WRITE);
    }

    public boolean isWritable() {
        return (this.accessMode == AccessMode.READ_WRITE);
    }

    // Helper factory methods
    public static FieldDefinition readOnly(final String fieldName) {
        return new FieldDefinition(fieldName, AccessMode.READ_ONLY);
    }

    public static FieldDefinition readWrite(final String fieldName) {
        return new FieldDefinition(fieldName, AccessMode.READ_WRITE);
    }
}
