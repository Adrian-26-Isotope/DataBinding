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
}
