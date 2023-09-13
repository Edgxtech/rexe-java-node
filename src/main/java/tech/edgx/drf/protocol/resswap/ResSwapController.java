package tech.edgx.drf.protocol.resswap;

import java.util.concurrent.CompletableFuture;
import kotlin.*;
import tech.edgx.drf.protocol.resswap.pb.MessageOuterClass;

public interface ResSwapController {

    void send(MessageOuterClass.Message msg);

    CompletableFuture<Unit> close();
}
