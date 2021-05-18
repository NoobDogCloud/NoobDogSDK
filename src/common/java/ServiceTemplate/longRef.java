package common.java.ServiceTemplate;

public class longRef {
    private long value;

    public longRef(long initValue) {
        value = initValue;
    }

    public void setValue(long i) {
        value = i;
    }

    public long longValue() {
        return value;
    }
}
