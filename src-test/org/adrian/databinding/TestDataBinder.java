package org.adrian.databinding;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

/**
 * Test-only utility that provides {@code reset} and {@code drainOnce} operations for the active {@link DataBinder}
 * without polluting the production API. Uses reflection to access private fields and methods that are intentionally
 * absent from the public surface.
 */
final class TestDataBinder {

    private static final Field TRANSMITTER_BINDINGS;
    private static final Field RECEIVER_BINDINGS;
    private static final Field CLEANER;
    private static final Field CLEANER_TRANSMITTER_MAP;
    private static final Field CLEANER_RECEIVER_MAP;
    private static final Field CLEANER_REFERENCE_QUEUE;
    private static final Method PROCESS_PHANTOM_REFERENCE;

    static {
        try {
            TRANSMITTER_BINDINGS = DataBinder.class.getDeclaredField("transmitterBindings");
            TRANSMITTER_BINDINGS.setAccessible(true);

            RECEIVER_BINDINGS = DataBinder.class.getDeclaredField("receiverBindings");
            RECEIVER_BINDINGS.setAccessible(true);

            CLEANER = DataBinder.class.getDeclaredField("cleaner");
            CLEANER.setAccessible(true);

            CLEANER_TRANSMITTER_MAP = DataBinderCleaner.class.getDeclaredField("transmitterMap");
            CLEANER_TRANSMITTER_MAP.setAccessible(true);

            CLEANER_RECEIVER_MAP = DataBinderCleaner.class.getDeclaredField("receiverMap");
            CLEANER_RECEIVER_MAP.setAccessible(true);

            CLEANER_REFERENCE_QUEUE = DataBinderCleaner.class.getDeclaredField("referenceQueue");
            CLEANER_REFERENCE_QUEUE.setAccessible(true);

            PROCESS_PHANTOM_REFERENCE =
                    DataBinderCleaner.class.getDeclaredMethod("processPhantomReference", PhantomReference.class);
            PROCESS_PHANTOM_REFERENCE.setAccessible(true);
        }
        catch (NoSuchFieldException | NoSuchMethodException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private TestDataBinder() {}

    /**
     * Clears all binding registrations and phantom-reference tracking on the active {@link DataBinder} instance.
     * Intended for test setup/teardown only.
     */
    @SuppressWarnings("unchecked")
    static void reset() {
        DataBinder binder = DataBinder.getActive();
        try {
            Map<Object, Object> transmitterBindings = (Map<Object, Object>) TRANSMITTER_BINDINGS.get(binder);
            transmitterBindings.clear();

            Map<Object, Object> receiverBindings = (Map<Object, Object>) RECEIVER_BINDINGS.get(binder);
            receiverBindings.clear();

            Object cleaner = CLEANER.get(binder);
            ConcurrentMap<Object, Object> transmitterMap =
                    (ConcurrentMap<Object, Object>) CLEANER_TRANSMITTER_MAP.get(cleaner);
            transmitterMap.clear();

            ConcurrentMap<Object, Object> receiverMap =
                    (ConcurrentMap<Object, Object>) CLEANER_RECEIVER_MAP.get(cleaner);
            receiverMap.clear();
        }
        catch (IllegalAccessException e) {
            throw new IllegalStateException("Failed to reset DataBinder", e);
        }
    }

    /**
     * Drains all currently-enqueued phantom references on the active {@link DataBinder} instance without blocking.
     * Processes each reference the same way the background daemon does, allowing tests to synchronously flush cleanup
     * instead of relying on the daemon thread's unbounded processing latency.
     *
     * @return the number of phantom references processed
     */
    @SuppressWarnings("unchecked")
    static int drainOnce() {
        DataBinder binder = DataBinder.getActive();
        try {
            Object cleaner = CLEANER.get(binder);
            ReferenceQueue<IBindable> referenceQueue = (ReferenceQueue<IBindable>) CLEANER_REFERENCE_QUEUE.get(cleaner);
            int count = 0;
            PhantomReference<IBindable> phantomRef;
            while ((phantomRef = (PhantomReference<IBindable>) referenceQueue.poll()) != null) {
                PROCESS_PHANTOM_REFERENCE.invoke(cleaner, phantomRef);
                count++;
            }
            return count;
        }
        catch (IllegalAccessException | java.lang.reflect.InvocationTargetException e) {
            throw new IllegalStateException("Failed to drain DataBinder cleaner queue", e);
        }
    }
}
