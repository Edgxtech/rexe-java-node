package tech.edgx.dee.protocol.cptswap;

import io.libp2p.core.Stream;
import kotlin.Unit;
import tech.edgx.dee.protocol.cptswap.pb.MessageOuterClass;

import java.util.concurrent.CompletableFuture;

public class CptswapConnection implements CptswapController {

    private final Stream conn;

    public CptswapConnection(Stream conn) {
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
