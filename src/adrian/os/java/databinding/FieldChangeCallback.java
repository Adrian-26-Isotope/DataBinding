package adrian.os.java.databinding;


@FunctionalInterface
public interface FieldChangeCallback {

    void onFieldChange(String fieldName, Object oldValue, Object newValue, UpdateChain chain);
}
