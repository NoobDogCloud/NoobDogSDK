import common.java.Cache.CacheHelper;
import common.java.Thread.ThreadHelper;
import junit.framework.TestCase;
import org.junit.Test;


public class NoTest_testDbAndCache extends TestCase {
    // 测试不带配置名本地缓存
    @Test
    public void testCacheLocal() {
        /*
        var t = TimeHelper.getNowTimestampByZero();
        ThreadHelper.sleep(1000);
        var c = TimeHelper.getNowTimestampByZero() - t;
        System.out.println(c);
        */

        CacheHelper ca = CacheHelper.buildLocal();
        ca.set("TestA", "putao520");
        assertEquals("putao520", ca.get("TestA"));
        ca.setExpire("TestA", 1000);    // 设置缓存过期时间1秒
        ThreadHelper.sleep(3000);
        assertNull(ca.get("TestA"));
    }
}
