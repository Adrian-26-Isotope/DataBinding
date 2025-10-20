package adrian.os.java.databinding.demo;

/**
 * Demonstration of the new declarative data binding approach.
 * Shows how easy it is to create data classes with different field access
 * patterns.
 */
public class DataBindingDemo {

    public static void main(final String[] args) {
        // Create master data
        MasterData master = new MasterData("initial", "initial", "initial");
        master.printAllFields();

        // Create slave data with full read-write access
        SlaveData1 slave1 = master.createSlaveData1();
        slave1.printAllFields();

        // Create slave data with read-only access
        SlaveData2 slave2 = master.createSlaveData2();
        slave2.printAllFields();

        // Create mapped data with different field names
        SlaveData3 slave3 = slave1.createSlaveData3();
        slave3.printAllFields();


        // Test bidirectional binding
        System.out.println("\n=== Testing Bidirectional Binding ===");

        // Change master, should propagate to all slaves
        master.setName("master");

        System.out.println("After master update:");
        System.out.println("Master name: " + master.getName());
        System.out.println("Slave1 name: " + slave1.getName());
        System.out.println("Slave2 name: " + slave2.getName());
        System.out.println("Slave3 name: " + slave3.getName());

        // Change slave1 (read-write), should propagate back to master
        slave1.setType("Slave1");

        System.out.println("\nAfter slave1 update:");
        System.out.println("Master type: " + master.getType());
        System.out.println("Slave1 type: " + slave1.getType());
        System.out.println("Slave3 type: " + slave3.getType());

        // Change mapped data, should propagate to others
        slave3.setNotes("Slave3");

        System.out.println("\nAfter salve3 update:");
        System.out.println("Master notes: " + master.getNotes());
        System.out.println("Slave1 notes: " + slave1.getNotes());
        System.out.println("Slave2 notes: " + slave2.getNotes());
        System.out.println("Slave3 notes: " + slave3.getNotes());


        // print all fields
        master.printAllFields();
        slave1.printAllFields();
        slave2.printAllFields();
        slave3.printAllFields();

    }
}
