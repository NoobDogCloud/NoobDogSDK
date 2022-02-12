package common.java.DataSource.DataSourceStore;

public class DataSourceReader {
    private int lastUnreadWater;
    private int position;

    private DataSourceReader() {
        lastUnreadWater = 0;
        position = 0;
    }

    public static DataSourceReader build() {
        return new DataSourceReader();
    }

    public int getLastUnreadWater() {
        return lastUnreadWater;
    }

    public void setLastUnreadWater(int lastUnreadWater) {
        this.position = this.lastUnreadWater;
        this.lastUnreadWater = lastUnreadWater;
    }

    public int getPosition() {
        return position;
    }


}
