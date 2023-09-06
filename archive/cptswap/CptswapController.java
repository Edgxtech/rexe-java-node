package tech.edgx.dee.protocol.cptswap;

import kotlin.Unit;
import tech.edgx.dee.protocol.cptswap.pb.MessageOuterClass;

import java.util.concurrent.CompletableFuture;

public interface CptswapController {

    void send(MessageOuterClass.Message msg);

    CompletableFuture<Unit> close();
}
