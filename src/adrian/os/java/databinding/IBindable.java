package adrian.os.java.databinding;

import java.util.UUID;

/**
 * Interface for objects that can participate in data binding operations.
 * Provides a unique identifier for tracking and managing binding relationships.
 */
public interface IBindable {

    /**
     * Gets the unique identifier for this bindable object.
     *
     * @return the unique UUID for this object
     */
    UUID getID();
}
