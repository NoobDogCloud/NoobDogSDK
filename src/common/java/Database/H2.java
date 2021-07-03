package common.java.Database;

public class H2 extends Sql {
    public H2(String configString) {
        super(configString);
    }

    public String getFullForm() {
        return '"' + (ownId == null || ownId.equals("") ? formName : formName + "_" + ownId) + '"';
    }
}
