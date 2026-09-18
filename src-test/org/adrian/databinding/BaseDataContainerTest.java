package org.adrian.databinding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.adrian.databinding.data.TestMasterData;
import org.adrian.databinding.data.TestSlaveData1;
import org.adrian.databinding.data.TestSlaveData2;
import org.adrian.databinding.data.TestSlaveData3;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BaseDataContainerTest {

    private TestMasterData master;
    private TestSlaveData1 slave1;
    private TestSlaveData2 slave2;
    private TestSlaveData3 slave3;

    @BeforeEach
    void setUp() {
        this.master = new TestMasterData("test", "type1", "notes1");
        this.slave1 = DataFactory.createFrom(TestSlaveData1.SCHEMA, this.master, TestSlaveData1::new);
        this.slave2 = DataFactory.createFrom(TestSlaveData2.SCHEMA, this.master, TestSlaveData2::new);
        this.slave3 = DataFactory.createFrom(TestSlaveData3.SCHEMA, this.slave1, TestSlaveData3::new);
    }

    @AfterAll
    static void tearDown() {
        // Reset the DataBinder after all tests to avoid interference with other tests
        TestDataBinder.reset();
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
            this.slave2.getFieldValue("nonExistentField", Object.class);
        });
    }

    @Test
    void testUniqueIds() {
        // Each data container should have a unique ID
        assertNotNull(this.master.getId());
        assertNotNull(this.slave1.getId());
        assertNotNull(this.slave2.getId());
        assertNotNull(this.slave3.getId());

        // IDs should be different
        assert !this.master.getId().equals(this.slave1.getId());
        assert !this.master.getId().equals(this.slave2.getId());
        assert !this.master.getId().equals(this.slave3.getId());
        assert !this.slave1.getId().equals(this.slave2.getId());
        assert !this.slave1.getId().equals(this.slave3.getId());
        assert !this.slave2.getId().equals(this.slave3.getId());
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
    void testReadOnly() {
        assertThrows(IllegalArgumentException.class,
                () -> this.slave2.setFieldValue(TestMasterData.NAME_FIELD, "expect exception"));
    }

    @Test
    void testUnknownField() {
        assertThrows(IllegalArgumentException.class, () -> this.master.getFieldValue("NO_FIELD", Object.class));
        assertThrows(IllegalArgumentException.class, () -> this.master.setFieldValue("NO_FIELD", "expect exception"));
    }

}
