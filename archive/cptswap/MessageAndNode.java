package tech.edgx.dee.protocol.cptswap;

import io.ipfs.cid.Cid;
import tech.edgx.dee.protocol.cptswap.pb.MessageOuterClass;

public class MessageAndNode {
    public final MessageOuterClass.Message msg;
    public final Cid nodeId;

    public MessageAndNode(MessageOuterClass.Message msg, Cid nodeId) {
        this.msg = msg;
        this.nodeId = nodeId;
    }
}
