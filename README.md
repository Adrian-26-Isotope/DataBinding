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
- **Declarative Schema Definition**: Define field access patterns using [`DataSchema`](src/org/adrian/databinding/DataSchema.java) and [`FieldDefinition`](src/org/adrian/databinding/FieldDefinition.java)
- **Automatic Binding**: Objects are automatically bound during creation via [`DataFactory`](src/org/adrian/databinding/DataFactory.java)
- **Cycle Prevention**: Built-in cycle detection prevents infinite update loops using [`UpdateChain`](src/org/adrian/databinding/UpdateChain.java)
- **Conflict Resolution**: Timestamp-based resolution for concurrent updates
- **Flexible Access Control**: Per-field read-only or read-write access
- **Memory Leak Prevention**: Automatic cleanup of bindings when objects are garbage collected

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Quick Start](#quick-start)
- [Architecture](#architecture)
- [Developer & Maintainer Guide](#developer--maintainer-guide)
- [Example](#example)
- [Requirements](#requirements)
- [License](#license)
- [Author](#author)

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

        // DONT do this - throws exception for read only fields
        // setFieldValue(NAME_FIELD, name);
        // setFieldValue(VALUE_FIELD, value);

        // DO this instead
        Map<String, Object> initialValues = new HashMap<>();
        initialValues.put(NAME_FIELD, name);
        initialValues.put(VALUE_FIELD, value);
        initValues(initialValues);
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

    public ReadOnlySlaveData(DataSchema schema, BaseDataContainer master) {
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

- **[`BaseDataContainer`](src/org/adrian/databinding/BaseDataContainer.java)**: Abstract base class providing all data binding functionality
- **[`DataSchema`](src/org/adrian/databinding/DataSchema.java)**: Defines field structure and access permissions
- **[`FieldDefinition`](src/org/adrian/databinding/FieldDefinition.java)**: Specifies individual field access modes
- **[`DataFactory`](src/org/adrian/databinding/DataFactory.java)**: Thread-safe factory for creating bound objects
- **[`DataBinder`](src/org/adrian/databinding/DataBinder.java)**: Central registry managing binding relationships
- **[`UpdateChain`](src/org/adrian/databinding/UpdateChain.java)**: Cycle detection and timestamp management
- **[`DataBinderCleaner`](src/org/adrian/databinding/DataBinderCleaner.java)** Deamon thread to ensure memory is cleaned properly

### Thread Safety

The framework uses several mechanisms to ensure thread safety:
- **Fine-grained locking**: Each field has its own `ReentrantReadWriteLock`
- **[`MultiLockManager`](src/org/adrian/databinding/MultiLockManager.java)**: Safely acquires multiple locks in consistent order
- **Atomic operations**: Field values and timestamps use atomic references
- **Lock ordering**: Prevents deadlocks in multi-field operations

### Consistency

The system provides strong eventual consistency guarantees - all linked data instances will converge to the same values, and the timestamp-based conflict resolution ensures a deterministic outcome when multiple threads update simultaneously.

However, it doesn't guarantee instantaneous consistency - there may be brief moments where linked objects have different values during update propagation. The design prioritizes avoiding deadlocks and infinite loops over strict immediate consistency.

For most practical applications, this approach provides adequate consistency while maintaining good performance and avoiding common concurrency pitfalls like deadlocks.

### Memory Management

The framework includes a sophisticated automatic cleanup mechanism to prevent memory leaks:

- **[`DataBinderCleaner`](src/org/adrian/databinding/DataBinderCleaner.java)**: Uses `PhantomReference` to detect when data objects are garbage collected.
- **[`WeakFieldChangeCallback`](src/org/adrian/databinding/WeakFieldChangeCallback.java)**: Wraps callbacks with weak references to prevent callback owners not being garbage collected.
- **Automatic Cleanup**: Background daemon thread continuously monitors and removes stale bindings.
- **Dual Tracking**: Separate tracking for transmitters and receivers ensures complete cleanup.

The cleanup system automatically removes:
- Transmitter bindings when the source object is garbage collected
- Receiver callbacks when the target object is garbage collected
- Expired weak references that point to collected objects

This ensures that the [`DataBinder`](src/org/adrian/databinding/DataBinder.java) cache doesn't prevent garbage collection of bound objects, preventing memory leaks in long-running applications.

## Developer & Maintainer Guide

This section documents the internal mechanisms behind the framework. It is intended for contributors and maintainers who need to understand *how* binding, propagation, concurrency, and cleanup work under the hood.

### Terminology: Transmitter vs Receiver

Every binding is a directed relationship between two objects for a specific field:

- **Transmitter** — the object *whose* field change triggers notification. When a transmitter's field is written, `DataBinder` looks up the transmitter's UUID in its forward index to find all registered callbacks.
- **Receiver** — the object that *gets notified* and updated. It is the target of the callback; its `onFieldChange` method is invoked with the new value.

In `DataBinder.bind(transmitter, fieldName, receiver, callback)`, the transmitter is the source and the receiver is the destination. In `BaseDataContainer.bindTo(transmitter, fieldName)`, the calling object (`this`) registers *itself* as the receiver of the transmitter's changes.

How this maps to master/slave in `DataFactory.setupBinding`:

| Call | Transmitter | Receiver | Direction |
|------|-------------|----------|-----------|
| `slave.bindTo(master, field)` | master | slave | master → slave |
| `master.bindTo(slave, field)` | slave | master | slave → master |

A `READ_WRITE` field produces *both* calls, so each object is simultaneously a transmitter and a receiver for that field — which is exactly why `DataBinderCleaner` tracks transmitter and receiver phantom references separately (see [Memory Management Lifecycle](#memory-management-lifecycle)). A `READ_ONLY` field only registers `slave.bindTo(master, ...)`, so the slave is purely a receiver and the master is purely a transmitter for that field.

### How a Field Update Propagates

When a field is set, a single `UpdateChain` travels through the entire binding graph. The full call path is:

```
user calls container.setFieldValue("field", value)          // public setter
  ├─ validates field isWritable()                           // READ_ONLY fields rejected here
  ├─ new UpdateChain(timestamp = System.nanoTime())         // one timestamp per write
  ├─ chain.add(this.id)
  └─ setFieldValue("field", value, chain)                   // internal setter
       ├─ acquire field write lock
       ├─ if chain.timestamp <= field.timestamp → REJECT    // stale / duplicate
       ├─ field.timestamp = chain.timestamp
       ├─ swap value via AtomicReference.getAndSet()
       ├─ release write lock
       └─ DataBinder.update(this, "field", oldValue, newValue, chain)
            ├─ if oldValue.equals(newValue) → SKIP           // no-op change
            └─ for each WeakFieldChangeCallback on (this, "field"):
                 └─ callback.execute(...)
                      ├─ if receiver already GC'd → mark expired, skip
                      └─ FieldChangeCallback.onFieldChange(receiver, "field", …, chain)
                           ├─ if chain.contains(receiver.id) → SKIP   // cycle break
                           ├─ chain.add(receiver.id)
                           └─ receiver.setFieldValue("field", newValue, chain)  // internal setter
                                └─ … recursion continues through the graph …
```

Key takeaways for maintainers:

- **One `UpdateChain` per external write.** Every external `setFieldValue` call creates a fresh chain with a single `nanoTime` timestamp. That timestamp is shared by *all* field updates that cascade from it, which is what lets the timestamp check reject re-entrant updates to the same field.
- **The notification happens outside the lock.** `DataBinder.update()` is called *after* the write lock is released, so callbacks never execute while a field lock is held. This avoids re-entrancy deadlocks.
- **`UpdateChain` is not thread-safe by design.** It uses a plain `HashSet` because each chain is propagated synchronously down a single call stack. It is never shared across threads.

### Two-Tier Setter Access Control

There are two `setFieldValue` paths, and they enforce access control differently:

| Path | Checks `isWritable()`? | Checks timestamp? | Triggers propagation? | Used by |
|------|:---:|:---:|:---:|---|
| `setFieldValue(String, Object)` — public | Yes | Yes (new chain) | Yes | User code |
| `setFieldValue(String, Object, UpdateChain)` — private | **No** | Yes | Yes | Propagation / callbacks |
| `initValues(Map)` — protected | No | No | **No** | Construction only |

The private setter deliberately skips the `isWritable()` check so that a `READ_ONLY` field on a slave *can* be updated by propagation from the master — it just can't be set directly by user code. `initValues` is the third path: it writes `AtomicReference` values directly, sets no timestamps, and fires no callbacks. It exists solely for construction-time initialization (and for `copyValuesFromMaster`).

### Schema-Driven Asymmetric Binding

Binding topology is determined entirely by the **slave's** schema, wired in [`DataFactory.setupBinding`](src/org/adrian/databinding/DataFactory.java):

```
for each readable field in slave's schema:  slave.bindTo(master, field)   // master → slave
for each writable field in slave's schema:  master.bindTo(slave, field)   // slave → master
```

| Slave field mode | Master → slave | Slave → master | Effect |
|------------------|:---:|:---:|---|
| `READ_ONLY` | yes | no | One-way: slave mirrors master, user can't write |
| `READ_WRITE` | yes | yes | Bidirectional sync |

Because `READ_WRITE` fields appear in *both* the readable and writable lists, they get two bindings — one in each direction. `bindTo` is package-private so that all binding setup flows through `DataFactory.createFrom`; external code cannot (and should not) wire bindings manually.

### The `DataBinder` Dual Index

[`DataBinder`](src/org/adrian/databinding/DataBinder.java) is a static singleton holding two indices over the same set of bindings:

- **Forward index** (`transmitterBindings`): `transmitter UUID → field name → list<WeakFieldChangeCallback>`. Used at notification time — when a transmitter's field changes, look up the callbacks to invoke.
- **Reverse index** (`receiverBindings`): `receiver UUID → list<BindingReference>`. Used at cleanup time — when a receiver is GC'd, quickly find every binding that points *at* it without scanning the forward index.

The inner `BindingReference` records `(transmitterId, transmitterFieldName, callback)` so a receiver's cleanup can walk back to the right slot in the forward index.

### Cycle Detection & Conflict Resolution

The `UpdateChain` serves two roles simultaneously:

1. **Cycle detection (UUID set).** Before applying an incoming update, `onFieldChange` checks `chain.contains(receiver.id)`. If the receiver is already in the chain, the update is silently dropped. This breaks A→B→A loops in cyclic topologies.
2. **Last-writer-wins (timestamp).** The internal setter compares `chain.timestamp` against the field's stored timestamp. If `chain.timestamp <= field.timestamp`, the update is rejected. Within a single chain this means each field is written at most once. Across two independent external writes (two separate chains), the later `nanoTime` wins and the earlier one is dropped — deterministic last-writer-wins.

### Memory Management Lifecycle

#### The Problem: DataBinder Outlives the Data Objects

[`DataBinder`](src/org/adrian/databinding/DataBinder.java) is a static singleton — it lives for the entire JVM lifetime. Every binding registers a `FieldChangeCallback` *strongly* in the forward index (`transmitter UUID → field → callback list`). If that callback held a strong reference to the receiver object, the receiver could never be garbage collected as long as `DataBinder` exists — even if the rest of the application has dropped all references to it. In a long-running application this would be a growing memory leak: every master/slave pair ever created would stay alive forever.

The framework solves this with two Java reference types that let the GC reclaim objects while still allowing `DataBinder` to *detect* that they are gone and clean up afterward.

#### Weak References: Letting the Receiver Be Collected

A `WeakReference` holds a reference to an object *without preventing* the GC from collecting it. As long as some other part of the application holds a strong reference to the object, `weakRef.get()` returns it. Once the last strong reference is gone, the GC is free to collect the object, and subsequent `weakRef.get()` calls return `null`.

[`WeakFieldChangeCallback`](src/org/adrian/databinding/WeakFieldChangeCallback.java) wraps each callback with a `WeakReference` to the **receiver** (the owner that should be notified). This breaks the strong link:

```
DataBinder (static, immortal)
  └─ strong → FieldChangeCallback (the lambda / method ref)
                └─ weak → receiver object   ← can be GC'd when nothing else holds it
```

When `DataBinder.update()` iterates callbacks, each `WeakFieldChangeCallback.execute()` checks `weakOwner.get()`. If the receiver is still alive, it invokes the callback normally. If the receiver was collected, `get()` returns `null` and the callback is marked **expired** — the update is skipped for that receiver, and the expired callback is removed from the list lazily. This prevents stale notifications to dead objects and gradually prunes the list.

> [!IMPORTANT] 
> **The no-capture constraint:** For this to work, the `FieldChangeCallback` itself must **not** capture the receiver. If it did, the strong reference chain would be `DataBinder → callback → receiver`, and the weak reference would be pointless. `BaseDataContainer.bindTo` passes `BaseDataContainer::onFieldChange` — a *static* method reference that receives the receiver as a **parameter**, not via capture. An instance method reference (`owner::onFieldChange`) or a lambda closing over `owner` would capture the receiver strongly and defeat the entire mechanism.

#### Phantom References: Post-GC Cleanup of the Index Itself

The weak-reference layer handles *individual callbacks* during `update()`, but it only fires when an update actually happens. If the receiver is GC'd but no transmitter ever writes again, the expired callback lingers in the list. Worse, the `DataBinder` indices themselves (`transmitterBindings`, `receiverBindings`) are keyed by UUID and hold `BindingReference` / `WeakFieldChangeCallback` objects — those entries also need to be cleaned up.

A `PhantomReference` is different from a `WeakReference` in two critical ways:

| | `WeakReference` | `PhantomReference` |
|---|---|---|
| `get()` returns the object? | Yes (until cleared) | **Always `null`** — by design |
| When is it enqueued? | Before finalization (object may still be revived) | **After** finalization (object is definitively dead) |
| Typical use | Accessing an object that *might* be collected | **Reacting** to an object being collected |

Because `PhantomReference.get()` is always `null`, the phantom ref cannot be used to access the object — only to *detect that it is gone*. The framework stores the UUID in a side map alongside the phantom reference, because the phantom ref itself provides no way back to the object's identity:

```
transmitterMap:  PhantomReference<IBindable> → UUID
receiverMap:     PhantomReference<IBindable> → UUID
```

[`DataBinderCleaner`](src/org/adrian/databinding/DataBinderCleaner.java) runs a virtual daemon thread that blocks on `referenceQueue.remove()`. When the GC collects an `IBindable` and enqueues its phantom reference, the thread wakes up, looks up the UUID from the side map, and calls `DataBinder.cleanupTransmitter()` or `DataBinder.cleanupReceiver()` to remove the corresponding entries from the indices. Because phantom refs are enqueued *after* finalization, this cleanup runs only when the object is truly unreachable — there is no risk of operating on a half-collected object.

After processing, `phantomRef.clear()` is called explicitly because — unlike weak/soft references — phantom references are **not** auto-cleared by the GC. Without `clear()`, the `PhantomReference` object would remain as a key in the side map indefinitely.

#### The Three Layers Together

| Layer | Reference type | What it guards | When cleanup fires | What it removes |
|------|---------------|----------------|---------------------|-----------------|
| 1. Callback expiry | `WeakReference` (receiver) | Individual callbacks | Lazily, during `DataBinder.update()` | Expired callbacks whose receiver was GC'd |
| 2. Receiver cleanup | `PhantomReference` (receiver) | Reverse-index entries | Proactively, via `DataBinderCleaner` daemon after GC | All bindings pointing *at* the GC'd receiver |
| 3. Transmitter cleanup | `PhantomReference` (transmitter) | Forward-index entries | Proactively, via `DataBinderCleaner` daemon after GC | The entire transmitter entry (all fields, all callbacks) |

Layer 1 is **lazy** — it only runs when a write happens, so it handles the common case efficiently. Layers 2 and 3 are **proactive** — they guarantee cleanup even if no writes ever occur again, closing the gap that lazy expiry alone would leave. The split between receiver and transmitter cleanup exists because a single object can be both (for a `READ_WRITE` field), so each role is tracked and cleaned independently.

### Concurrency Model

| Layer | Mechanism | Notes |
|------|-----------|-------|
| Per-field value | `AtomicReference<Object>` | Lock-free reads/writes |
| Per-field timestamp | `AtomicLong` | Init `0L` |
| Per-field access | `ReentrantReadWriteLock` | Multiple readers / single writer |
| Multi-field snapshot | [`MultiLockManager`](src/org/adrian/databinding/MultiLockManager.java) | Acquires in caller-given order, releases LIFO |
| Binding registry | `ConcurrentHashMap` + `CopyOnWriteArrayList` | Lock-free reads, copy-on-write iteration |

`MultiLockManager.lockAll` acquires locks in the order the caller passes them and releases in reverse order. Deadlock avoidance depends on callers using a consistent order — in practice this is the schema's field-definition order (deterministic). Note that `lockAll` swallows exceptions and returns an empty list on partial failure (marked `// TODO` in source), so a mid-acquisition failure currently proceeds without holding the earlier locks.

### Key Invariants for Maintainers

- `DataFactory.createFrom` is the **only** entry point that sets up bindings. Don't wire bindings manually.
- `initValues` bypasses timestamps and propagation — use it only during construction. Calling it after binding is established will create silent inconsistencies (value present, timestamp `0`, no propagation).
- The public setter enforces `isWritable()`; the private setter (propagation path) does not. Don't "fix" this asymmetry — it's how `READ_ONLY` fields receive master updates.
- `DataBinderCleaner` runs on a virtual thread and blocks on `referenceQueue.remove()`. Tests that depend on cleanup (e.g. `DataBinderCleanupTest`) force GC and sleep, so they can be timing-sensitive.
- Field lookup in `DataSchema` is O(n). Fine for small schemas; revisit if schemas grow large.

## Example

See [`DataBindingDemo`](src/org/adrian/databinding/demo/DataBindingDemo.java) for a complete working example:

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
