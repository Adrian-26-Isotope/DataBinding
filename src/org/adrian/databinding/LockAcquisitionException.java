package org.adrian.databinding;

/**
 * Thrown when {@link MultiLockManager#lockAll} fails to acquire one or more
 * locks. Any locks acquired before the failure are released by
 * {@code lockAll} before this exception is thrown.
 */
public class LockAcquisitionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new lock acquisition exception with the specified detail
     * message and cause.
     *
     * @param message the detail message
     * @param cause the exception that caused the lock acquisition to fail
     */
    public LockAcquisitionException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
