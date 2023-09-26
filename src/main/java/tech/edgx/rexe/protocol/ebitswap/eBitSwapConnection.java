package tech.edgx.rexe.protocol.ebitswap;

import io.libp2p.core.Stream;
import kotlin.Unit;
import tech.edgx.rexe.protocol.ebitswap.pb.MessageOuterClass;

import java.util.concurrent.CompletableFuture;

public class eBitSwapConnection implements eBitSwapController {

    private final Stream conn;

    public eBitSwapConnection(Stream conn) {
        this.conn = conn;
    }

    @Override
    public void send(MessageOuterClass.Message msg) {
        conn.writeAndFlush(msg);
    }

    @Override
    public CompletableFuture<Unit> close() {
        return conn.close();
    }
}
