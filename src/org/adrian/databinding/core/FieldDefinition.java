package org.adrian.databinding.core;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Defines a field with its access permissions and provides factory methods
 * for creating common field access patterns.
 */
public class FieldDefinition {

    /**
     * Enumeration defining the access modes for fields. Modes are combined via
     * {@link EnumSet}&lt;{@link AccessMode}&gt; to express arbitrary combinations
     * (e.g. {@code EnumSet.of(AccessMode.READ, AccessMode.WRITE)}).
     */
    @SuppressWarnings("javadoc")
    public enum AccessMode {
                            READ,
                            WRITE
    }

    private final String fieldName;
    private final Set<AccessMode> accessModes;
    private final Class<?> type;

    /**
     * Creates a new field definition with the specified name, access modes, and value type.
     * The type is used for write-time validation in {@code setFieldValue} / {@code initValues}
     * and for type-checked reads via {@code getFieldValue(name, type)}. Pass
     * {@code Object.class} to accept any value type.
     *
     * @param fieldName the name of the field
     * @param accessModes the access permissions for the field
     * @param type the expected runtime type of the field's value
     */
    public FieldDefinition(final String fieldName, final EnumSet<AccessMode> accessModes, final Class<?> type) {
        this.fieldName = Objects.requireNonNull(fieldName, "fieldName");
        this.accessModes = Collections.unmodifiableSet(EnumSet.copyOf(Objects.requireNonNull(accessModes, "accessModes")));
        this.type = Objects.requireNonNull(type, "type");
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
     * Gets the access modes of this field.
     *
     * @return an unmodifiable set of access modes
     */
    public Set<AccessMode> getAccessModes() {
        return this.accessModes;
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
        return this.accessModes.contains(AccessMode.READ);
    }

    /**
     * Checks if this field can be written to.
     *
     * @return true if the field is writable
     */
    public boolean isWritable() {
        return this.accessModes.contains(AccessMode.WRITE);
    }

    /**
     * Factory method for creating a read-only field definition with a concrete value type.
     *
     * @param fieldName the name of the field
     * @param type the expected runtime type of the field's value
     * @return a read-only field definition
     */
    public static FieldDefinition readOnly(final String fieldName, final Class<?> type) {
        return new FieldDefinition(fieldName, EnumSet.of(AccessMode.READ), type);
    }

    /**
     * Factory method for creating a read-write field definition with a concrete value type.
     *
     * @param fieldName the name of the field
     * @param type the expected runtime type of the field's value
     * @return a read-write field definition
     */
    public static FieldDefinition readWrite(final String fieldName, final Class<?> type) {
        return new FieldDefinition(fieldName, EnumSet.of(AccessMode.READ, AccessMode.WRITE), type);
    }
}
