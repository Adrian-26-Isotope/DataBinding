package adrian.os.java.databinding;

/**
 * Functional interface for building data objects
 */
@FunctionalInterface
public interface DataObjectBuilder<T extends BaseDataContainer> {

    T build(BaseDataContainer master, DataSchema schema);
}
