package tech.edgx.drf.protocol.resswap;

import io.libp2p.core.Stream;
import kotlin.Unit;
//import org.peergos.protocol.bitswap.pb.MessageOuterClass;
import tech.edgx.drf.protocol.resswap.pb.MessageOuterClass;

import java.util.concurrent.CompletableFuture;

public class ResSwapConnection implements ResSwapController {

    private final Stream conn;

    public ResSwapConnection(Stream conn) {
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
