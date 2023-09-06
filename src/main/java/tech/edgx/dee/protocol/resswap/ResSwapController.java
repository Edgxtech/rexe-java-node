package tech.edgx.dee.protocol.resswap;

import java.util.concurrent.CompletableFuture;
import kotlin.*;
import tech.edgx.dee.protocol.resswap.pb.MessageOuterClass;

public interface ResSwapController {

    void send(MessageOuterClass.Message msg);

    CompletableFuture<Unit> close();
}
