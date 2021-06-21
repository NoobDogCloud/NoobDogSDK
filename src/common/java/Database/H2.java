package common.java.Database;

public class H2 extends Sql {
    public H2(String configString) {
        super(configString);
    }

    public String getfullform() {
        return '"' + (ownid == null || ownid.equals("") ? formName : formName + "_" + ownid) + '"';
    }
}
