import common.java.Http.Client.WebSocketClient;
import junit.framework.TestCase;

public class TestWebsocketClient extends TestCase {
    public void test_base(){
        WebSocketClient wsc = WebSocketClient.build("ws://127.0.0.1:805");
        wsc.connect();
        wsc.onReceive( v-> System.out.println(v) );
        wsc.onReconnect(ch-> System.out.println("重新连接了"));

        String sub_msg = "{\n" +
                "    \"path\":\"/system/context/get/1\",\n" +
                "    \"header\":{\n" +
                "        \"appKey\":\"grapeSoft@\",\n" +
                "        \"mode\":\"subscribe\",\n" +
                "        \"host\":\"127.0.0.1:805\"\n" +
                "    },\n" +
                "    \"param\":{}\n" +
                "}";
        // 订阅
        wsc.send(sub_msg);
    }
}
