package tech.edgx.protocol.dpswap;

import kotlin.Unit;
import tech.edgx.protocol.dpswap.pb.MessageOuterClass;

import java.util.concurrent.CompletableFuture;

public interface DpswapController {

    void send(MessageOuterClass.Message msg);

    CompletableFuture<Unit> close();
}
