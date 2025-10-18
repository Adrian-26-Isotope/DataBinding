package adrian.os.java.databinding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import adrian.os.java.databinding.demo.MasterData;
import adrian.os.java.databinding.demo.SlaveData1;
import adrian.os.java.databinding.demo.SlaveData2;
import adrian.os.java.databinding.demo.SlaveData3;

class BaseDataContainerTest {

    private MasterData master;
    private SlaveData1 slave1;
    private SlaveData2 slave2;
    private SlaveData3 slave3;

    @BeforeEach
    void setUp() {
        this.master = new MasterData("test", "type1", "notes1");
        this.slave1 = this.master.createSlaveData1();
        this.slave2 = this.master.createSlaveData2();
        this.slave3 = this.slave1.createSlaveData3();
    }

    @Test
    void testMasterDataCreation() {
        assertNotNull(this.master);
        assertEquals("test", this.master.getName());
        assertEquals("type1", this.master.getType());
        assertEquals("notes1", this.master.getNotes());
    }

    @Test
    void testSlaveDataCreation() {
        assertNotNull(this.slave1);
        assertNotNull(this.slave2);
        assertNotNull(this.slave3);

        // Test that slave1 inherited values from master
        assertEquals("test", this.slave1.getName());
        assertEquals("type1", this.slave1.getType());
        assertEquals("notes1", this.slave1.getNotes());
        assertEquals("initial", this.slave1.getAdditionalInfo());

        // Test that slave2 inherited values from master
        assertEquals("test", this.slave2.getName());
        assertEquals("notes1", this.slave2.getNotes());

        // Test that slave3 inherited values from slave1 & master
        assertEquals("test", this.slave3.getName());
        assertEquals("type1", this.slave3.getType());
        assertEquals("notes1", this.slave3.getNotes());
        assertEquals("initial", this.slave3.getAdditionalInfo());
    }

    @Test
    void testBidirectionalBinding() {
        // Change master, should propagate to slaves
        this.master.setName("newName");

        assertEquals("newName", this.master.getName());
        assertEquals("newName", this.slave1.getName());
        assertEquals("newName", this.slave2.getName());
        assertEquals("newName", this.slave3.getName());

        // Change slave1 (writable), should propagate back to master
        this.slave1.setType("newType");

        assertEquals("newType", this.master.getType());
        assertEquals("newType", this.slave1.getType());
        assertEquals("newType", this.slave3.getType());
    }

    @Test
    void testFieldAccessRestrictions() {
        // Test that accessing non-existent or non-readable fields throws exception
        assertThrows(IllegalArgumentException.class, () -> {
            this.slave2.getFieldValue("nonExistentField");
        });
    }

    @Test
    void testUniqueIds() {
        // Each data container should have a unique ID
        assertNotNull(this.master.getID());
        assertNotNull(this.slave1.getID());
        assertNotNull(this.slave2.getID());
        assertNotNull(this.slave3.getID());

        // IDs should be different
        assert !this.master.getID().equals(this.slave1.getID());
        assert !this.master.getID().equals(this.slave2.getID());
        assert !this.master.getID().equals(this.slave3.getID());
        assert !this.slave1.getID().equals(this.slave2.getID());
        assert !this.slave1.getID().equals(this.slave3.getID());
        assert !this.slave2.getID().equals(this.slave3.getID());
    }

    @Test
    void testAdditionalFieldInSlave() {
        // slave1 has an additional field that master doesn't have
        String val = "extraInfo";
        this.slave1.setAdditionalInfo(val);
        assertEquals(val, this.slave1.getAdditionalInfo());
        assertEquals(val, this.slave3.getAdditionalInfo());
    }

    @Test
    void testUnbindingData() {
        // Verify initial binding works
        this.master.setName("boundName");
        assertEquals("boundName", this.slave1.getName());
        assertEquals("boundName", this.slave2.getName());
        assertEquals("boundName", this.slave3.getName());

        // Unbind slave1 "name" field
        DataBinder.unbindAll(this.slave1, "name");

        // Change slave1 name
        this.slave1.setName("unboundName");

        // slave1 does not send name updates anymore
        assertEquals("boundName", this.master.getName());
        assertEquals("unboundName", this.slave1.getName());
        assertEquals("boundName", this.slave2.getName());
        assertEquals("boundName", this.slave3.getName());

        // However, if we change masters name, the change should still propagate to all
        // its slaves
        this.master.setName("masterName");
        assertEquals("masterName", this.master.getName());
        assertEquals("masterName", this.slave1.getName());
        assertEquals("masterName", this.slave2.getName());

        // slave3 is a slave of slave1. since slave1, does not propagate name changes,
        // slave3 does not see the change
        // from master
        assertEquals("boundName", this.slave3.getName());

        // Verify other fields are still bound
        this.slave1.setType("stillBoundType");
        assertEquals("stillBoundType", this.master.getType());
        assertEquals("stillBoundType", this.slave1.getType());
        assertEquals("stillBoundType", this.slave3.getType());
    }

    @Test
    void testSingleCallbackUnbinding() {
        // Create a test callback to track field changes
        final StringBuilder callbackLog = new StringBuilder();
        FieldChangeCallback testCallback = (fieldName, oldValue, newValue, chain) -> {
            callbackLog.append(String.format("Field: %s, Old: %s, New: %s; ", fieldName, oldValue, newValue));
        };

        // Bind the callback to the master's "name" field
        DataBinder.bind(this.master, "name", testCallback);

        // Change the name to trigger the callback
        this.master.setName("firstChange");
        assertEquals("Field: name, Old: test, New: firstChange; ", callbackLog.toString());

        // Reset the log
        callbackLog.setLength(0);

        // Change the name again to verify callback is still active
        this.master.setName("secondChange");
        assertEquals("Field: name, Old: firstChange, New: secondChange; ", callbackLog.toString());

        // Reset the log
        callbackLog.setLength(0);

        // Unbind the specific callback
        DataBinder.unbind(this.master, "name", testCallback);

        // Change the name again - callback should not be triggered
        this.master.setName("thirdChange");
        assertEquals("", callbackLog.toString()); // Should be empty since callback was removed

        // Verify that the actual field change still works
        assertEquals("thirdChange", this.master.getName());
        assertEquals("thirdChange", this.slave1.getName());
        assertEquals("thirdChange", this.slave2.getName());
    }

    @Test
    void testCompleteUnbinding() {
        // Verify initial binding works
        this.master.setName("initialName");
        this.master.setType("initialType");
        assertEquals("initialName", this.slave1.getName());
        assertEquals("initialType", this.slave1.getType());
        assertEquals("initialName", this.slave2.getName());
        assertEquals("initialName", this.slave3.getName());
        assertEquals("initialType", this.slave3.getType());

        // Completely unbind slave1
        DataBinder.unbindAll(this.slave1);

        // noone should receive these updates
        this.slave1.setName("newName");
        this.slave1.setType("newType");
        assertEquals("newName", this.slave1.getName());
        assertEquals("newType", this.slave1.getType());
        assertEquals("initialName", this.master.getName());
        assertEquals("initialType", this.master.getType());
        assertEquals("initialName", this.slave2.getName());
        assertEquals("initialName", this.slave3.getName());
        assertEquals("initialType", this.slave3.getType());

        // slave1 still receives changes
        this.master.setName("independentName");
        this.slave3.setNotes("slaveNote");
        assertEquals("independentName", this.slave1.getName());
        assertEquals("slaveNote", this.slave1.getNotes());
    }
}
