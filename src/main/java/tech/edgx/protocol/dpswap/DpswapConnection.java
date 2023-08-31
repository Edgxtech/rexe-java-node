package tech.edgx.protocol.dpswap;

import io.libp2p.core.Stream;
import kotlin.Unit;
import tech.edgx.protocol.dpswap.pb.MessageOuterClass;

import java.util.concurrent.CompletableFuture;

public class DpswapConnection implements DpswapController {

    private final Stream conn;

    public DpswapConnection(Stream conn) {
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
