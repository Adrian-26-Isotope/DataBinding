package org.adrian.databinding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.adrian.databinding.core.DataBinder;
import org.adrian.databinding.core.DataSchema;
import org.adrian.databinding.core.FieldDefinition;
import org.adrian.databinding.core.TestDataBinder;
import org.adrian.databinding.data.TestMasterData;
import org.adrian.databinding.data.TestSlaveData1;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Concurrency regression tests for the data-binding framework. Each test
 * exercises a concurrency-sensitive code path that would break if a prior fix
 * were reverted.
 *
 * <ul>
 * <li><b>Test 1</b> &mdash; concurrent {@code bind()}: 100 bindings created
 * from 4 threads; verifies all callbacks propagate.</li>
 * <li><b>Test 2</b> &mdash; concurrent {@code setFieldValue} on the same
 * field: 8 writers; verifies master and slave always agree.</li>
 * <li><b>Test 3</b> &mdash; concurrent {@code setFieldValue} on different
 * fields: 3 writers; verifies all fields propagate independently.</li>
 * <li><b>Test 4</b> &mdash; concurrent {@code DataFactory.createFrom}: 80 slave
 * creations; verifies no deadlock and consistent snapshots.</li>
 * <li><b>Test 5</b> &mdash; concurrent {@code bind()} + {@code cleanup()}: the
 * HIGH-1 race; 50 rounds of bind-vs-cleanup on the same source entry.</li>
 * </ul>
 */
class ConcurrencyTest {

    private static final String FIELD = "field";
    private static final DataSchema SCHEMA = new DataSchema(
            FieldDefinition.readWrite(FIELD, String.class));

    @BeforeEach
    void setUp() {
        TestDataBinder.reset();
    }

    @AfterEach
    void tearDown() {
        TestDataBinder.reset();
    }

    /**
     * 4 threads each create 25 slaves bound to the same master. After all
     * threads complete, updating the master must propagate to all 100 slaves.
     * Catches regressions where concurrent {@code bind()} calls corrupt the
     * binding indices or lose callbacks.
     */
    @Test
    void testConcurrentBindAllCallbacksPropagate() throws InterruptedException {
        final int threadCount = 4;
        final int slavesPerThread = 25;
        final ConcContainer master = new ConcContainer(SCHEMA);

        final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        final ConcurrentLinkedQueue<Throwable> errors = new ConcurrentLinkedQueue<>();
        final List<ConcContainer> allSlaves = new ArrayList<>();
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch doneLatch = new CountDownLatch(threadCount);

        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < slavesPerThread; i++) {
                        ConcContainer slave = DataFactory.createFrom(SCHEMA, master, ConcContainer::new);
                        synchronized (allSlaves) {
                            allSlaves.add(slave);
                        }
                    }
                } catch (Throwable e) {
                    errors.add(e);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(30, TimeUnit.SECONDS), "Worker threads timed out");
        executor.shutdown();

        assertTrue(errors.isEmpty(), "Worker threads threw: " + errors);
        assertEquals(threadCount * slavesPerThread, allSlaves.size());

        master.set("updated");
        for (ConcContainer slave : allSlaves) {
            assertEquals("updated", slave.get(), "Slave did not receive update");
        }
    }

    /**
     * 8 threads each write 100 unique values to the same field on a master
     * with a bound slave. After all threads complete, master and slave must
     * hold the same value. Catches regressions in the {@code AtomicLong}
     * timestamp counter (HIGH-4/MED-6) and the per-field write lock.
     */
    @Test
    void testConcurrentSetFieldValueSameFieldConsistent() throws InterruptedException {
        final ConcContainer master = new ConcContainer(SCHEMA);
        final ConcContainer slave = DataFactory.createFrom(SCHEMA, master, ConcContainer::new);

        final int threadCount = 8;
        final int writesPerThread = 100;

        final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        final ConcurrentLinkedQueue<Throwable> errors = new ConcurrentLinkedQueue<>();
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch doneLatch = new CountDownLatch(threadCount);

        for (int t = 0; t < threadCount; t++) {
            final int tid = t;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < writesPerThread; i++) {
                        master.set("thread-" + tid + "-" + i);
                    }
                } catch (Throwable e) {
                    errors.add(e);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(30, TimeUnit.SECONDS), "Worker threads timed out");
        executor.shutdown();

        assertTrue(errors.isEmpty(), "Worker threads threw: " + errors);
        assertEquals(master.get(), slave.get(),
                "Master and slave disagree after concurrent writes");
    }

    /**
     * 3 threads concurrently write to 3 different fields on a master with a
     * bound slave. After all threads complete, each field on the slave must
     * match the corresponding field on the master. Catches regressions in
     * {@code update()} thread-safety when notifying callbacks on different
     * fields of the same transmitter.
     */
    @Test
    void testConcurrentSetFieldValueDifferentFields() throws InterruptedException {
        final TestMasterData master = new TestMasterData("name0", "type0", "notes0");
        final TestSlaveData1 slave = DataFactory.createFrom(TestSlaveData1.SCHEMA, master, TestSlaveData1::new);

        final int writesPerThread = 50;
        final ExecutorService executor = Executors.newFixedThreadPool(3);
        final ConcurrentLinkedQueue<Throwable> errors = new ConcurrentLinkedQueue<>();
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch doneLatch = new CountDownLatch(3);

        executor.submit(() -> {
            try {
                startLatch.await();
                for (int i = 0; i < writesPerThread; i++) {
                    master.setName("name" + i);
                }
            } catch (Throwable e) {
                errors.add(e);
            } finally {
                doneLatch.countDown();
            }
        });

        executor.submit(() -> {
            try {
                startLatch.await();
                for (int i = 0; i < writesPerThread; i++) {
                    master.setType("type" + i);
                }
            } catch (Throwable e) {
                errors.add(e);
            } finally {
                doneLatch.countDown();
            }
        });

        executor.submit(() -> {
            try {
                startLatch.await();
                for (int i = 0; i < writesPerThread; i++) {
                    master.setNotes("notes" + i);
                }
            } catch (Throwable e) {
                errors.add(e);
            } finally {
                doneLatch.countDown();
            }
        });

        startLatch.countDown();
        assertTrue(doneLatch.await(30, TimeUnit.SECONDS), "Worker threads timed out");
        executor.shutdown();

        assertTrue(errors.isEmpty(), "Worker threads threw: " + errors);
        assertEquals(master.getName(), slave.getName());
        assertEquals(master.getType(), slave.getType());
        assertEquals(master.getNotes(), slave.getNotes());
    }

    /**
     * 8 threads each create 10 slaves from the same master via
     * {@code DataFactory.createFrom}. All threads must complete without
     * deadlock, and every slave must have a consistent snapshot of the master's
     * field values. Catches regressions in {@code MultiLockManager} (CRIT-1)
     * and lock-ordering (LOW-7).
     */
    @Test
    void testConcurrentCreateFromNoDeadlock() throws InterruptedException {
        final TestMasterData master = new TestMasterData("name", "type", "notes");
        final int threadCount = 8;
        final int createsPerThread = 10;

        final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        final ConcurrentLinkedQueue<Throwable> errors = new ConcurrentLinkedQueue<>();
        final List<TestSlaveData1> allSlaves = new ArrayList<>();
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch doneLatch = new CountDownLatch(threadCount);

        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < createsPerThread; i++) {
                        TestSlaveData1 slave =
                                DataFactory.createFrom(TestSlaveData1.SCHEMA, master, TestSlaveData1::new);
                        synchronized (allSlaves) {
                            allSlaves.add(slave);
                        }
                    }
                } catch (Throwable e) {
                    errors.add(e);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(30, TimeUnit.SECONDS), "Worker threads timed out \u2014 possible deadlock");
        executor.shutdown();

        assertTrue(errors.isEmpty(), "Worker threads threw: " + errors);
        assertEquals(threadCount * createsPerThread, allSlaves.size());

        for (TestSlaveData1 slave : allSlaves) {
            assertEquals(master.getName(), slave.getName(), "Slave has torn snapshot");
            assertEquals(master.getType(), slave.getType(), "Slave has torn snapshot");
            assertEquals(master.getNotes(), slave.getNotes(), "Slave has torn snapshot");
        }
    }

    /**
     * The HIGH-1 race: 50 rounds of concurrent {@code bind()} vs
     * {@code cleanup()} on the same source entry. In each round, one old slave
     * is cleaned up (removing its callback and triggering {@code cleanup()})
     * while a new slave is being bound (adding a callback). If the fix is
     * reverted, the new slave's callback can be orphaned.
     */
    @Test
    void testConcurrentBindWhileCleanup() throws InterruptedException {
        final ConcContainer master = new ConcContainer(SCHEMA);
        final int rounds = 50;
        final List<ConcContainer> newSlaves = new ArrayList<>();
        final ConcurrentLinkedQueue<Throwable> errors = new ConcurrentLinkedQueue<>();

        for (int round = 0; round < rounds; round++) {
            final ConcContainer oldSlave = DataFactory.createFrom(SCHEMA, master, ConcContainer::new);
            final UUID oldSlaveId = oldSlave.getId();

            final CountDownLatch startLatch = new CountDownLatch(1);
            final CountDownLatch doneLatch = new CountDownLatch(1);

            Thread bindThread = new Thread(() -> {
                try {
                    startLatch.await();
                    ConcContainer newSlave = DataFactory.createFrom(SCHEMA, master, ConcContainer::new);
                    synchronized (newSlaves) {
                        newSlaves.add(newSlave);
                    }
                } catch (Throwable e) {
                    errors.add(e);
                } finally {
                    doneLatch.countDown();
                }
            });
            bindThread.start();

            startLatch.countDown();
            TestDataBinder.forceCleanupReceiver(oldSlaveId);

            assertTrue(doneLatch.await(5, TimeUnit.SECONDS),
                    "Bind thread timed out in round " + round);
            bindThread.join();
        }

        assertTrue(errors.isEmpty(), "Bind threads threw: " + errors);
        assertEquals(rounds, newSlaves.size());

        master.set("updated");
        for (ConcContainer slave : newSlaves) {
            assertEquals("updated", slave.get(),
                    "New slave was orphaned by bind/cleanup race");
        }
    }

    /**
     * Minimal container with a single read-write {@code String} field, used
     * for concurrency tests that only need binding propagation.
     */
    static final class ConcContainer extends BasicDataContainer {

        ConcContainer(final DataSchema schema) {
            super(schema, DataBinder.getActive(), new HashMap<>());
        }

        ConcContainer(final DataSchema schema, final BasicDataContainer master) {
            super(schema, master.getBinder(), new HashMap<>());
        }

        void set(final String value) {
            setFieldValue(FIELD, value);
        }

        String get() {
            return getFieldValue(FIELD, String.class);
        }
    }
}
