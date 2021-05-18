package common.java.ServiceTemplate;

public class onFind {
    private String[] ids;
    private String[] field;

    public onFind(String[] ids, String[] field) {
        this.ids = ids;
        this.field = field;
    }

    public String[] getField() {
        return field;
    }

    public void setField(String[] field) {
        this.field = field;
    }

    public String[] getIds() {
        return ids;
    }

    public void setIds(String[] ids) {
        this.ids = ids;
    }
}
