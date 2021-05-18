package common.java.ServiceTemplate;

/*
 * 模拟按址方式传递int类型参数
 * */
public class intRef {
    private int value;

    public intRef(int initValue) {
        value = initValue;
    }

    public void setValue(int i) {
        value = i;
    }

    public int intValue() {
        return value;
    }
}
