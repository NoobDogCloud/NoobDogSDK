package common.java.Rpc;

import java.util.ArrayList;
import java.util.List;

public class RpcJsonFilterBlockGroup {
    private final List<RpcJsonFilterBlock> blocks;

    private RpcJsonFilterBlockGroup() {
        blocks = new ArrayList<>(1);
    }

    public static RpcJsonFilterBlockGroup build() {
        return new RpcJsonFilterBlockGroup();
    }

    public RpcJsonFilterBlockGroup add(RpcJsonFilterBlock block) {
        blocks.add(block);
        return this;
    }

    public FilterReturn forEach(RpcJsonFilterCheckerCallback fn) {
        FilterReturn r = FilterReturn.success();
        for (RpcJsonFilterBlock block : blocks) {
            r = fn.run(block);
            if (!r.isSuccess()) {
                break;
            }
        }
        return r;
    }
}
