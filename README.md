# DataBinding

A thread-safe Java data binding framework that enables automatic bidirectional synchronization between data objects with different field access patterns and schemas.

## Overview

This framework provides a declarative approach to data binding where you can:
- Create master data objects with full field access
- Generate slave objects with customized field access (read-only, read-write, or subset of fields)
- Automatically synchronize changes bidirectionally between bound objects
- Handle concurrent updates with timestamp-based conflict resolution
- Prevent infinite update loops with cycle detection

## Features

- **Thread-Safe**: All operations are thread-safe using fine-grained locking
- **Declarative Schema Definition**: Define field access patterns using [`DataSchema`](src/adrian/os/java/databinding/DataSchema.java) and [`FieldDefinition`](src/adrian/os/java/databinding/FieldDefinition.java)
- **Automatic Binding**: Objects are automatically bound during creation via [`DataFactory`](src/adrian/os/java/databinding/DataFactory.java)
- **Cycle Prevention**: Built-in cycle detection prevents infinite update loops using [`UpdateChain`](src/adrian/os/java/databinding/UpdateChain.java)
- **Conflict Resolution**: Timestamp-based resolution for concurrent updates
- **Flexible Access Control**: Per-field read-only or read-write access

## Quick Start

### 1. Define Your Master Data Class

```java
public class MyMasterData extends BaseDataContainer {
    public static final String NAME_FIELD = "name";
    public static final String VALUE_FIELD = "value";

    public static final DataSchema SCHEMA = new DataSchema(
        FieldDefinition.readWrite(NAME_FIELD),
        FieldDefinition.readWrite(VALUE_FIELD)
    );

    public MyMasterData(String name, int value) {
        super(SCHEMA);
        setFieldValue(NAME_FIELD, name);
        setFieldValue(VALUE_FIELD, value);
    }

    public String getName() { return getFieldValue(NAME_FIELD); }
    public void setName(String name) { setFieldValue(NAME_FIELD, name); }

    public Integer getValue() { return getFieldValue(VALUE_FIELD); }
    public void setValue(Integer value) { setFieldValue(VALUE_FIELD, value); }
}
```

### 2. Create Slave Data Classes

```java
public class ReadOnlySlaveData extends BaseDataContainer {
    public static final DataSchema SCHEMA = new DataSchema(
        FieldDefinition.readOnly(MyMasterData.NAME_FIELD),
        FieldDefinition.readOnly(MyMasterData.VALUE_FIELD)
    );

    public ReadOnlySlaveData(BaseDataContainer master, DataSchema schema) {
        super(schema, master);
    }

    public String getName() { return getFieldValue(MyMasterData.NAME_FIELD); }
    public Integer getValue() { return getFieldValue(MyMasterData.VALUE_FIELD); }
    // No setters - read-only access
}
```

### 3. Use the Factory to Create Bound Objects

```java
// Create master data
MyMasterData master = new MyMasterData("initial", 42);

// Create bound slave objects
ReadOnlySlaveData slave = DataFactory.createFrom(
    master,
    ReadOnlySlaveData.SCHEMA,
    ReadOnlySlaveData::new
);

// Changes to master automatically propagate to slave
master.setName("updated");
System.out.println(slave.getName()); // Prints: "updated"
```

## Architecture

### Core Components

- **[`BaseDataContainer`](src/adrian/os/java/databinding/BaseDataContainer.java)**: Abstract base class providing all data binding functionality
- **[`DataSchema`](src/adrian/os/java/databinding/DataSchema.java)**: Defines field structure and access permissions
- **[`FieldDefinition`](src/adrian/os/java/databinding/FieldDefinition.java)**: Specifies individual field access modes
- **[`DataFactory`](src/adrian/os/java/databinding/DataFactory.java)**: Thread-safe factory for creating bound objects
- **[`DataBinder`](src/adrian/os/java/databinding/DataBinder.java)**: Central registry managing binding relationships
- **[`UpdateChain`](src/adrian/os/java/databinding/UpdateChain.java)**: Cycle detection and timestamp management

### Thread Safety

The framework uses several mechanisms to ensure thread safety:
- **Fine-grained locking**: Each field has its own `ReentrantReadWriteLock`
- **[`MultiLockManager`](src/adrian/os/java/databinding/MultiLockManager.java)**: Safely acquires multiple locks in consistent order
- **Atomic operations**: Field values and timestamps use atomic references
- **Lock ordering**: Prevents deadlocks in multi-field operations

### Consistency

The system provides strong eventual consistency guarantees - all linked data instances will converge to the same values, and the timestamp-based conflict resolution ensures a deterministic outcome when multiple threads update simultaneously.

However, it doesn't guarantee instantaneous consistency - there may be brief moments where linked objects have different values during update propagation. The design prioritizes avoiding deadlocks and infinite loops over strict immediate consistency.

For most practical applications, this approach provides adequate consistency while maintaining good performance and avoiding common concurrency pitfalls like deadlocks.

## Example

See [`DataBindingDemo`](src/adrian/os/java/databinding/demo/DataBindingDemo.java) for a complete working example:

```console
=== Data Container: MasterData (ID: 01d7e1d7-cc23-403c-917e-0410258c8890) ===
  name: initial (timestamp: 11623897004900)
  type: initial (timestamp: 11623899582500)
  notes: initial (timestamp: 11623899621600)

=== Data Container: SlaveData1 (ID: d57a996b-a932-45d7-a462-fb321e76f442) ===
  name: initial (timestamp: 0)
  type: initial (timestamp: 0)
  notes: initial (timestamp: 0)
  additionalInfo: initial (timestamp: 11623925742800)

=== Data Container: SlaveData2 (ID: 161281fc-5681-49ac-a310-d908b73943b9) ===
  name: initial (timestamp: 0)
  notes: initial (timestamp: 0)

=== Data Container: SlaveData3 (ID: f7ecf8bb-5a94-4535-bac0-454ebdd1c973) ===
  name: initial (timestamp: 0)
  notes: initial (timestamp: 0)
  type: initial (timestamp: 0)
  additionalInfo: initial (timestamp: 0)

=== Testing Bidirectional Binding ===
After master update:
Master name: master
Slave1 name: master
Slave2 name: master
Slave3 name: master

After slave1 update:
Master type: Slave1
Slave1 type: Slave1
Slave3 type: Slave1

After salve3 update:
Master notes: Slave3
Slave1 notes: Slave3
Slave2 notes: Slave3
Slave3 notes: Slave3

=== Data Container: MasterData (ID: 01d7e1d7-cc23-403c-917e-0410258c8890) ===
  name: master (timestamp: 11623929741200)
  type: Slave1 (timestamp: 11623931280200)
  notes: Slave3 (timestamp: 11623931633400)

=== Data Container: SlaveData1 (ID: d57a996b-a932-45d7-a462-fb321e76f442) ===
  name: master (timestamp: 11623929741200)
  type: Slave1 (timestamp: 11623931280200)
  notes: Slave3 (timestamp: 11623931633400)
  additionalInfo: initial (timestamp: 11623925742800)

=== Data Container: SlaveData2 (ID: 161281fc-5681-49ac-a310-d908b73943b9) ===
  name: master (timestamp: 11623929741200)
  notes: Slave3 (timestamp: 11623931633400)

=== Data Container: SlaveData3 (ID: f7ecf8bb-5a94-4535-bac0-454ebdd1c973) ===
  name: master (timestamp: 11623929741200)
  notes: Slave3 (timestamp: 11623931633400)
  type: Slave1 (timestamp: 11623931280200)
  additionalInfo: initial (timestamp: 0)


```

## Requirements
- Java 21 or higher

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Author

Adrian-26-Isotope
