package tech.edgx.rexe.protocol.ebitswap;

import io.ipfs.cid.Cid;
import tech.edgx.rexe.protocol.ebitswap.pb.MessageOuterClass;

public class MessageAndNode {
    public final MessageOuterClass.Message msg;
    public final Cid nodeId;

    public MessageAndNode(MessageOuterClass.Message msg, Cid nodeId) {
        this.msg = msg;
        this.nodeId = nodeId;
    }
}
