package tech.edgx.dee.protocol.dpswap;

import kotlin.Unit;
import tech.edgx.dee.protocol.dpswap.pb.MessageOuterClass;

import java.util.concurrent.CompletableFuture;

public interface DpswapController {

    void send(MessageOuterClass.Message msg);

    CompletableFuture<Unit> close();
}
