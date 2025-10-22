package adrian.os.java.databinding;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Simple test to demonstrate and verify the garbage collection cleanup functionality.
 * This test creates containers, establishes bindings, then allows objects to be
 * garbage collected and verifies that the DataBinder cache is cleaned up.
 */
public class DataBinderCleanupTest {

    @Test
    void testCleanup() throws InterruptedException {
        System.out.println("Testing Garbage Collection Cleanup...");

        // Create some test containers that will be garbage collected
        createAndBindContainers();

        // Force garbage collection
        System.out.println("Forcing garbage collection...");
        System.gc();
        System.gc(); // Call twice to increase likelihood of collection

        // Give the cleanup thread time to process
        Thread.sleep(1000);

        System.out.println("Monitored containers: " + DataBinderCleaner.getMonitoredContainerCount());
        assertEquals(0, DataBinderCleaner.getMonitoredContainerCount());
        assertEquals(0, DataBinder.getTransmitterCount());
        assertEquals(0, DataBinder.getReceiverCount());
        System.out.println("Test completed. Check that containers were cleaned up.");
    }

    private void createAndBindContainers() {
        // Create a simple schema for testing
        DataSchema schema = new DataSchema(new FieldDefinition("testField", FieldDefinition.AccessMode.READ_WRITE));

        // Create containers (these will go out of scope after this method)
        TestContainer container1 = new TestContainer(schema);
        TestContainer container2 = new TestContainer(schema);

        // Create some bindings
        container1.bindTo(container2, "testField");
        container2.bindTo(container1, "testField");

        System.out.println("Created containers with IDs: " + container1.getID() + ", " + container2.getID());

        container1.setTestField("1st");
        assertEquals("1st", container2.getTestField());
        container2.setTestField("2nd");
        assertEquals("2nd", container1.getTestField());

        System.out.println("Initial monitored containers: " + DataBinderCleaner.getMonitoredContainerCount());
        assertEquals(2, DataBinder.getReceiverCount());
        assertEquals(2, DataBinder.getTransmitterCount());
        assertEquals(4, DataBinderCleaner.getMonitoredContainerCount()); // 2 for transmitter, 2 for receiver
    }


    /**
     * Simple test implementation of BaseDataContainer
     */
    private static class TestContainer extends BaseDataContainer {

        public TestContainer(final DataSchema schema) {
            super(schema);
        }

        public void setTestField(final Object value) {
            setFieldValue("testField", value);
        }

        public Object getTestField() {
            return getFieldValue("testField");
        }
    }
}
