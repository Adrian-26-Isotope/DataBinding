package adrian.os.java.databinding;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Utility class for managing multiple ReentrantReadWriteLocks concurrently.
 * Provides methods to safely acquire and release multiple locks in a consistent
 * order
 * to prevent deadlocks and ensure thread safety in multi-lock scenarios.
 */
public class MultiLockManager {

    /**
     * Enumeration defining the types of locks that can be acquired.
     */
    public enum LockType {
                          READ,
                          WRITE;
    }

    /**
     * Locks multiple ReentrantReadWriteLocks in the given order using the specified
     * lock type.
     * If any exception occurs during lock acquisition, all previously acquired
     * locks are released.
     *
     * @param type the type of lock to acquire (READ or WRITE)
     * @param locks variable number of ReentrantReadWriteLock objects to lock
     * @return list of acquired locks in the same order (for unlocking)
     */
    public static List<Lock> lockAll(final LockType type, final ReentrantReadWriteLock... locks) {
        if ((locks == null) || (locks.length == 0)) {
            return new ArrayList<>();
        }

        List<Lock> acquiredLocks = new ArrayList<>();

        try {
            // Lock each lock in the given order
            for (ReentrantReadWriteLock lock : locks) {
                if (lock != null) {
                    Lock lock2 = switch (type) {
                        case READ -> lock.readLock();
                        case WRITE -> lock.writeLock();
                    };
                    if (lock2 == null) {
                        continue;
                    }
                    lock2.lock();
                    acquiredLocks.add(lock2);
                }
            }
            return acquiredLocks;
        }
        catch (Exception e) {
            // If any exception occurs, unlock all previously acquired locks
            unlockAll(acquiredLocks);
            // TODO exception handling
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Unlocks all locks (read or write) in reverse order (LIFO).
     * This method safely handles null values and exceptions during unlock
     * operations.
     *
     * @param locks list of Lock objects (can be read or write locks)
     */
    public static void unlockAll(final List<? extends Lock> locks) {
        if ((locks == null) || locks.isEmpty()) {
            return;
        }

        // Unlock in reverse order (LIFO)
        for (int i = locks.size() - 1; i >= 0; i--) {
            Lock lock = locks.get(i);
            if (lock != null) {
                try {
                    lock.unlock();
                }
                catch (Exception e) {
                    // TODO exception handling
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Convenience method that automatically unlocks locks when used with
     * try-with-resources.
     * Returns an AutoCloseable that will unlock all acquired locks when closed.
     *
     * @param type the type of lock to acquire (READ or WRITE)
     * @param locks variable number of ReentrantReadWriteLock objects to lock
     * @return AutoCloseable that will unlock all acquired locks
     */
    public static AutoCloseable lockAllWithAutoUnlock(final LockType type, final ReentrantReadWriteLock... locks) {
        List<Lock> acquiredLocks = lockAll(type, locks);
        return () -> unlockAll(acquiredLocks);
    }
}
