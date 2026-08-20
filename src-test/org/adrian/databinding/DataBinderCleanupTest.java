package org.adrian.databinding;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Simple test to demonstrate and verify the garbage collection cleanup functionality.
 * This test creates containers, establishes bindings, then allows objects to be
 * garbage collected and verifies that the DataBinder cache is cleaned up.
 */
public class DataBinderCleanupTest {

    private static final int MAX_GC_RETRIES = 20;

    private static final long GC_RETRY_SLEEP_MS = 100L;

    @BeforeEach
    void setUp() {
        TestDataBinder.reset();
    }

    @Test
    void testCleanup() throws InterruptedException {
        System.out.println("Testing Garbage Collection Cleanup...");

        // Create some test containers that will be garbage collected
        createAndBindContainers();

        // Deterministically drain phantom references: give GC multiple bounded
        // chances and synchronously process any enqueued refs instead of relying
        // on the daemon thread's unbounded latency.
        for (int i = 0; i < MAX_GC_RETRIES; i++) {
            System.gc();
            TestDataBinder.drainOnce();
            if (DataBinder.getMonitoredContainerCount() == 0) {
                break;
            }
            Thread.sleep(GC_RETRY_SLEEP_MS);
        }

        System.out.println("Monitored containers: " + DataBinder.getMonitoredContainerCount());
        assertEquals(0, DataBinder.getMonitoredContainerCount());
        assertEquals(0, DataBinder.getTransmitterCount());
        assertEquals(0, DataBinder.getReceiverCount());
        System.out.println("Test completed. Check that containers were cleaned up.");
    }

    private void createAndBindContainers() {
        // Create a simple schema for testing
        DataSchema schema = new DataSchema(new FieldDefinition("testField", FieldDefinition.AccessMode.READ_WRITE));

        // Create containers (these will go out of scope after this method)
        TestContainer container1 = new TestContainer(schema);
        TestContainer container2 = DataFactory.createFrom(container1, schema, TestContainer::new);
        System.out.println("Created containers with IDs: " + container1.getID() + ", " + container2.getID());

        container1.setTestField("1st");
        assertEquals("1st", container2.getTestField());
        container2.setTestField("2nd");
        assertEquals("2nd", container1.getTestField());

        System.out.println("Initial monitored containers: " + DataBinder.getMonitoredContainerCount());
        assertEquals(2, DataBinder.getReceiverCount());
        assertEquals(2, DataBinder.getTransmitterCount());
        assertEquals(4, DataBinder.getMonitoredContainerCount()); // 2 for transmitter, 2 for receiver
    }


    /**
     * Simple test implementation of BaseDataContainer
     */
    private static class TestContainer extends BaseDataContainer {

        public TestContainer(final DataSchema schema) {
            super(schema);
        }

        public TestContainer(final DataSchema schema, final BaseDataContainer master) {
            super(schema, master);
        }

        public void setTestField(final Object value) {
            setFieldValue("testField", value);
        }

        public Object getTestField() {
            return getFieldValue("testField");
        }
    }
}
