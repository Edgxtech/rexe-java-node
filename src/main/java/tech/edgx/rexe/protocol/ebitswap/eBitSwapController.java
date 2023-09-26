package tech.edgx.rexe.protocol.ebitswap;

import java.util.concurrent.CompletableFuture;
import kotlin.*;
import tech.edgx.rexe.protocol.ebitswap.pb.MessageOuterClass;

public interface eBitSwapController {

    void send(MessageOuterClass.Message msg);

    CompletableFuture<Unit> close();
}
