package org.adrian.databinding;

import org.adrian.databinding.core.DataSchema;

/**
 * Functional interface for building data objects from a master container and
 * schema.
 * Used by the DataFactory to create new instances with proper initialization.
 *
 * @param <T> the type of BasicDataContainer to build
 */
@FunctionalInterface
public interface DataObjectBuilder<T extends BasicDataContainer> {

    /**
     * Builds a new data object instance from the given schema and master.
     *
     * @param schema the schema defining the structure of the new object
     * @param master the master data container to copy values from
     * @return a new data object instance
     */
    T build(DataSchema schema, BasicDataContainer master);
}
