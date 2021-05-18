package common.java.Coordination.Common;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.json.gsc.JSONObject;

import java.nio.charset.StandardCharsets;

/**
 * 操作值
 * 1:成功
 * 0:失败
 * <p>
 * >=0x10000 具体事件
 * 1:挂载点更新
 * 2:挂载点数据变更
 * 7:订阅挂载点
 * 8:取消订阅挂载点
 */
public class GscCenterPacket {
    private final String key;                       // 值所在key
    private final JSONObject val;                   // 值
    private final short eventId;                    // 事件值
    private short status;                     // 状态值  0 失败,1 成功

    private GscCenterPacket(String key, JSONObject val, short eventId, boolean status) {
        this.key = key;
        this.val = val;
        this.eventId = eventId;
        this.status = (short) (status ? 1 : 0);
    }

    public static GscCenterPacket build(String key, JSONObject val, short eventId, boolean status) {
        return new GscCenterPacket(key, val, eventId, status);
    }

    public static GscCenterPacket build(ByteBuf buffer) {
        // 获得eventId
        short eventId = buffer.readShort();
        // 获得状态值
        short status = buffer.readShort();
        // 获得key长度
        short key_length = buffer.readShort();
        // 获得val字节集
        byte[] key_byte = new byte[key_length];
        buffer.readBytes(key_byte);
        String key_str = new String(key_byte, StandardCharsets.UTF_8);
        // 获得val长度
        int val_length = buffer.readInt();
        // 获得val字节集
        byte[] array = new byte[val_length];
        buffer.readBytes(array);
        String val_str = new String(array, StandardCharsets.UTF_8);
        /*
        int tlen = val_str.getBytes(StandardCharsets.UTF_8).length;
        // 测试打印最后4个字节值
        int tl = val_length-4;
        byte[] tE = new byte[4];
        byte[] tS = new byte[4];
        for(int i = 0; i < 4; i++){
            tS[i] = array[i];
            tE[i] = array[i+tl];
        }
        System.out.println("字符串长度:" + val_str.length());
        System.out.println("开头:" + new String(tS));
        System.out.println("结尾:" + new String(tE));
         */
        return new GscCenterPacket(key_str, JSONObject.build(val_str), eventId, status == (short) 1);
    }

    public JSONObject getData() {
        return val;
    }

    public boolean getStatus() {
        return this.status == (short) 1;
    }

    public GscCenterPacket setStatus(boolean status) {
        this.status = (short) (status ? 1 : 0);
        return this;
    }

    public short getEventId() {
        return this.eventId;
    }

    public String getKey() {
        return this.key;
    }

    public ByteBuf toByteBuf() {
        byte[] bKey = key.getBytes(StandardCharsets.UTF_8);
        short lKey = (short) bKey.length;
        String sVal = val.toString();
        byte[] bVal = sVal.getBytes(StandardCharsets.UTF_8);
        int lVal = bVal.length;
        // 测试打印最后4个字节值
        /*
        int tl = lVal-4;
        byte[] tE = new byte[4];
        byte[] tS = new byte[4];
        for(int i = 0; i < 4; i++){
            tS[i] = bVal[i];
            tE[i] = bVal[i+tl];
        }
        System.out.println("字符串长度:" + sVal.length());
        System.out.println("开头:" + new String(tS));
        System.out.println("结尾:" + new String(tE));
         */
        int lPacket = (4 + 4) +                          // 头和包长度
                (2 + 2) +                  // 事件id和状态值
                (2 + lKey) +               // 数据key长度和key
                (4 + lVal)                // 数据长度和数据
                ;
        return Unpooled.buffer(lPacket).writeBytes("GSC_".getBytes(StandardCharsets.UTF_8))
                .writeInt(lPacket)
                .writeShort(eventId)
                .writeShort(status)
                .writeShort(lKey)
                .writeBytes(bKey)
                .writeInt(lVal)
                .writeBytes(bVal);
    }
}
