
package org.adrian.databinding;

import java.util.HashMap;
import java.util.Map;

/**
 * Convenience base class for 'master' data containers that do not inherit from
 * another container. Provides constructors that capture the active
 * {@link DataBinder} and optionally set initial values via
 * {@link BasicDataContainer#initValues} during construction.
 */
public  class BasicMasterContainer extends BasicDataContainer {

    /**
     * Constructor for 'master' data instances that don't inherit from other containers. Captures the thread-local
     * active {@link DataBinder} instance at construction time.
     *
     * @param schema the schema defining the fields and their access permissions
     */
    protected BasicMasterContainer(final DataSchema schema) {
        this(schema, DataBinder.getActive());
    }

    /**
     * Constructor for 'master' data instances with initial values. Captures the
     * thread-local active {@link DataBinder} instance at construction time.
     *
     * @param schema the schema defining the fields and their access permissions
     * @param initial the initial field values to set during construction; may be empty
     */
    protected BasicMasterContainer(final DataSchema schema, final Map<String, Object> initial) {
        this(schema, DataBinder.getActive(), initial);
    }

    /**
     * Constructor for 'master' data instances that don't inherit from other containers, using an explicit
     * {@link DataBinder} instance.
     *
     * @param schema the schema defining the fields and their access permissions
     * @param binder the {@link DataBinder} instance this container registers its bindings with
     */
    protected BasicMasterContainer(final DataSchema schema, final DataBinder binder) {
        this(schema, binder, new HashMap<>());
    }

    /**
     * Constructor for 'master' data instances with explicit {@link DataBinder} and initial values.
     *
     * @param schema the schema defining the fields and their access permissions
     * @param binder the {@link DataBinder} instance this container registers its bindings with
     * @param initial the initial field values to set during construction; may be empty
     */
    protected BasicMasterContainer(final DataSchema schema, final DataBinder binder,
            final Map<String, Object> initial) {
        super(schema, binder, initial);
    }

}
