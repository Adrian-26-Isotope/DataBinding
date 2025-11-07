package adrian.os.java.databinding;

/**
 * Functional interface for building data objects from a master container and
 * schema.
 * Used by the DataFactory to create new instances with proper initialization.
 *
 * @param <T> the type of BaseDataContainer to build
 */
@FunctionalInterface
public interface DataObjectBuilder<T extends BaseDataContainer> {

    /**
     * Builds a new data object instance from the given master and schema.
     *
     * @param master the master data container to copy values from
     * @param schema the schema defining the structure of the new object
     * @return a new data object instance
     */
    T build(DataSchema schema, BaseDataContainer master);
}
