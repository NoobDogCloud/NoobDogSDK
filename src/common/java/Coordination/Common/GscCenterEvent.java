package common.java.Coordination.Common;

public class GscCenterEvent {
    public final static short DataInit = 0x00000001;     // 数据全局强制覆盖更新
    public final static short Insert = 0x00000010;       // 新增N行数据
    public final static short Update = 0x00000011;       // 更新N行数据(包含key)
    public final static short Delete = 0x00000012;       // 删除N行数据(包含key)
    public final static short Clear = 0x00000014;   // 数据全局强制完全删除
    public final static short Subscribe = 0x00000007;
    public final static short UnSubscribe = 0x00000008;

    public final static short TestDisconnect = 0x0000000f0;
    public final static short HeartPing = 0x000000fe;
    public final static short HeartPong = 0x000000ff;
}
