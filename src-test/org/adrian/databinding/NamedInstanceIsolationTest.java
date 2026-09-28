package org.adrian.databinding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.adrian.databinding.core.DataBinder;
import org.adrian.databinding.core.DataSchema;
import org.adrian.databinding.core.FieldDefinition;
import org.adrian.databinding.core.Scope;
import org.adrian.databinding.core.TestDataBinder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Verifies that the multiton and thread-local active-instance mechanism in
 * {@link DataBinder} provides true isolation between named instances.
 */
class NamedInstanceIsolationTest {

    private static final String FIELD = "field";
    private static final DataSchema SCHEMA = new DataSchema(FieldDefinition.readWrite(FIELD, String.class));

    @AfterEach
    void tearDown() {
        DataBinder.remove("sameName");
        DataBinder.remove("diffA");
        DataBinder.remove("diffB");
        DataBinder.remove("testActive");
        DataBinder.remove("outer");
        DataBinder.remove("inner");
        DataBinder.remove("binderCheck");
        DataBinder.remove("isoA");
        DataBinder.remove("isoB");
        DataBinder.remove("toRemove");
        DataBinder.remove("recreate");
        DataBinder.remove("threadA");
        DataBinder.remove("threadB");
        TestDataBinder.reset();
    }

    @Test
    void testGetReturnsSameInstanceForSameName() {
        DataBinder first = DataBinder.get("sameName");
        DataBinder second = DataBinder.get("sameName");
        assertSame(first, second);
    }

    @Test
    void testGetReturnsDifferentInstancesForDifferentNames() {
        DataBinder a = DataBinder.get("diffA");
        DataBinder b = DataBinder.get("diffB");
        assertNotSame(a, b);
    }

    @Test
    void testSetActiveDirectsGetActive() {
        assertSame(DataBinder.get(DataBinder.DEFAULT_INSTANCE), DataBinder.getActive());
        try (Scope scope = DataBinder.setActive("testActive")) {
            assertSame(DataBinder.get("testActive"), DataBinder.getActive());
        }
        assertSame(DataBinder.get(DataBinder.DEFAULT_INSTANCE), DataBinder.getActive());
    }

    @Test
    void testNestedScopesRestorePreviousName() {
        try (Scope outer = DataBinder.setActive("outer")) {
            assertSame(DataBinder.get("outer"), DataBinder.getActive());
            try (Scope inner = DataBinder.setActive("inner")) {
                assertSame(DataBinder.get("inner"), DataBinder.getActive());
            }
            assertSame(DataBinder.get("outer"), DataBinder.getActive());
        }
        assertSame(DataBinder.get(DataBinder.DEFAULT_INSTANCE), DataBinder.getActive());
    }

    @Test
    void testContainerCapturesActiveBinder() {
        try (Scope scope = DataBinder.setActive("binderCheck")) {
            IsoContainer container = new IsoContainer(SCHEMA);
            assertSame(DataBinder.get("binderCheck"), container.getBinder());
        }
    }

    @Test
    void testBindingIsolationBetweenNamedInstances() {
        IsoContainer masterA;
        IsoContainer slaveA;

        try (Scope scopeA = DataBinder.setActive("isoA")) {
            masterA = new IsoContainer(SCHEMA);
            slaveA = DataFactory.createFrom(SCHEMA, masterA, IsoContainer::new);
            masterA.set("valueA");
            assertEquals("valueA", slaveA.get());
        }

        IsoContainer masterB;
        IsoContainer slaveB;

        try (Scope scopeB = DataBinder.setActive("isoB")) {
            masterB = new IsoContainer(SCHEMA);
            slaveB = DataFactory.createFrom(SCHEMA, masterB, IsoContainer::new);
            masterB.set("valueB");
            assertEquals("valueB", slaveB.get());
        }

        masterA.set("changedA");
        assertEquals("changedA", slaveA.get());
        assertEquals("valueB", slaveB.get());

        masterB.set("changedB");
        assertEquals("changedB", slaveB.get());
        assertEquals("changedA", slaveA.get());
    }

    @Test
    void testRemoveShutsDownInstance() {
        DataBinder instance = DataBinder.get("toRemove");
        IsoContainer master = new IsoContainer(SCHEMA, instance);
        IsoContainer slave = DataFactory.createFrom(SCHEMA, master, IsoContainer::new);

        master.set("before");
        assertEquals("before", slave.get());

        DataBinder.remove("toRemove");

        assertThrows(IllegalStateException.class, () -> master.set("after"));
        assertThrows(IllegalStateException.class, () -> instance.bind(master, FIELD, slave, event -> {
        }));
    }

    @Test
    void testGetAfterRemoveCreatesFreshInstance() {
        DataBinder first = DataBinder.get("recreate");
        DataBinder.remove("recreate");
        DataBinder second = DataBinder.get("recreate");
        assertNotSame(first, second);
    }

    @Test
    void testThreadLocalIsolationBetweenThreads() throws InterruptedException {
        DataBinder binderA = DataBinder.get("threadA");
        DataBinder binderB = DataBinder.get("threadB");

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<DataBinder> otherThreadBinder = new AtomicReference<>();

        try (Scope scopeA = DataBinder.setActive("threadA")) {
            assertSame(binderA, DataBinder.getActive());

            Thread thread = new Thread(() -> {
                try (Scope scopeB = DataBinder.setActive("threadB")) {
                    otherThreadBinder.set(DataBinder.getActive());
                } finally {
                    latch.countDown();
                }
            });
            thread.start();
            assertTrue(latch.await(5, TimeUnit.SECONDS));

            assertSame(binderA, DataBinder.getActive());
        }

        assertSame(binderB, otherThreadBinder.get());
    }

    /**
     * Simple test container with a single read-write String field.
     */
    static final class IsoContainer extends BasicDataContainer {

        IsoContainer(final DataSchema schema) {
            super(schema, DataBinder.getActive(), new HashMap<>());
        }

        IsoContainer(final DataSchema schema, final DataBinder binder) {
            super(schema, binder, new HashMap<>());
        }

        IsoContainer(final DataSchema schema, final BasicDataContainer master) {
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
