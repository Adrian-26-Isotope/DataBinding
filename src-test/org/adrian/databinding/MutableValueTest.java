package org.adrian.databinding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

/**
 * Tests for the mutable-value wrapping mechanism in {@link BaseDataContainer#getFieldValue}.
 * Verifies that JDK collections, arrays, and {@link Copyable} custom types are
 * wrapped on read to prevent in-place mutation from bypassing the binding
 * propagation contract.
 */
class MutableValueTest {

    private static final String ITEMS_FIELD = "items";
    private static final DataSchema RW_LIST_SCHEMA = new DataSchema(FieldDefinition.readWrite(ITEMS_FIELD, List.class));
    private static final DataSchema RO_LIST_SCHEMA = new DataSchema(FieldDefinition.readOnly(ITEMS_FIELD, List.class));

    private static final String ARRAY_FIELD = "array";
    private static final DataSchema RW_ARRAY_SCHEMA =
            new DataSchema(FieldDefinition.readWrite(ARRAY_FIELD, String[].class));

    private static final String CUSTOM_FIELD = "custom";
    private static final DataSchema RW_CUSTOM_SCHEMA =
            new DataSchema(FieldDefinition.readWrite(CUSTOM_FIELD, MutableData.class));
    private static final DataSchema RO_CUSTOM_SCHEMA =
            new DataSchema(FieldDefinition.readOnly(CUSTOM_FIELD, MutableData.class));

    /**
     * Simple mutable custom object that implements {@link Copyable} so that
     * {@code getFieldValue} returns a defensive copy.
     */
    static final class MutableData implements Copyable<MutableData> {

        private String value;

        MutableData(final String value) {
            this.value = value;
        }

        String getValue() {
            return this.value;
        }

        void setValue(final String value) {
            this.value = value;
        }

        @Override
        public MutableData copy() {
            return new MutableData(this.value);
        }
    }

    /**
     * Test container supporting List, array, and Copyable fields with both
     * master and slave constructors.
     */
    static final class MutableTestContainer extends BaseDataContainer {

        MutableTestContainer(final DataSchema schema) {
            super(schema);
        }

        MutableTestContainer(final DataSchema schema, final BaseDataContainer master) {
            super(schema, master);
        }

        void setItems(final List<String> items) {
            setFieldValue(ITEMS_FIELD, items);
        }

        @SuppressWarnings("unchecked")
        List<String> getItems() {
            return getFieldValue(ITEMS_FIELD, List.class);
        }

        void setArray(final String[] array) {
            setFieldValue(ARRAY_FIELD, array);
        }

        String[] getArray() {
            return getFieldValue(ARRAY_FIELD, String[].class);
        }

        void setCustom(final MutableData custom) {
            setFieldValue(CUSTOM_FIELD, custom);
        }

        MutableData getCustom() {
            return getFieldValue(CUSTOM_FIELD, MutableData.class);
        }
    }

    @AfterAll
    static void tearDown() {
        TestDataBinder.reset();
    }

    @Test
    void testListFieldReturnsUnmodifiableView() {
        MutableTestContainer container = new MutableTestContainer(RW_LIST_SCHEMA);
        List<String> items = new ArrayList<>(List.of("a", "b"));
        container.setItems(items);

        List<String> returned = container.getItems();
        assertThrows(UnsupportedOperationException.class, () -> returned.add("c"));
    }

    @Test
    void testReadOnlySlaveCannotMutateMasterViaList() {
        MutableTestContainer master = new MutableTestContainer(RW_LIST_SCHEMA);
        List<String> items = new ArrayList<>(List.of("a", "b"));
        master.setItems(items);

        MutableTestContainer slave = DataFactory.createFrom(master, RO_LIST_SCHEMA, MutableTestContainer::new);

        List<String> slaveView = slave.getItems();
        assertThrows(UnsupportedOperationException.class, () -> slaveView.add("c"));
        assertEquals(2, master.getItems().size());
        assertEquals(2, slave.getItems().size());
    }

    @Test
    void testArrayReturnsDefensiveCopy() {
        MutableTestContainer container = new MutableTestContainer(RW_ARRAY_SCHEMA);
        String[] original = { "a", "b" };
        container.setArray(original);

        String[] returned = container.getArray();
        assertNotSame(original, returned);
        returned[0] = "MUTATED";

        String[] stored = container.getArray();
        assertEquals("a", stored[0]);
    }

    @Test
    void testCopyableCustomTypeReturnsCopy() {
        MutableTestContainer container = new MutableTestContainer(RW_CUSTOM_SCHEMA);
        MutableData data = new MutableData("initial");
        container.setCustom(data);

        MutableData returned = container.getCustom();
        assertNotSame(data, returned);
        returned.setValue("mutated");

        MutableData stored = container.getCustom();
        assertEquals("initial", stored.getValue());
    }

    @Test
    void testReadOnlySlaveCannotMutateMasterViaCopyable() {
        MutableTestContainer master = new MutableTestContainer(RW_CUSTOM_SCHEMA);
        master.setCustom(new MutableData("initial"));

        MutableTestContainer slave = DataFactory.createFrom(master, RO_CUSTOM_SCHEMA, MutableTestContainer::new);

        MutableData slaveView = slave.getCustom();
        slaveView.setValue("mutated");

        assertEquals("initial", master.getCustom().getValue());
        assertEquals("initial", slave.getCustom().getValue());
    }

    @Test
    void testUnmodifiableViewReflectsSetFieldValue() {
        MutableTestContainer container = new MutableTestContainer(RW_LIST_SCHEMA);
        container.setItems(new ArrayList<>(List.of("a")));

        container.setItems(new ArrayList<>(List.of("x", "y")));

        List<String> returned = container.getItems();
        assertEquals(List.of("x", "y"), returned);
    }
}
