package tech.edgx.dee.protocol.cptswap;

import java.util.concurrent.CompletableFuture;
import kotlin.*;
import tech.edgx.dee.protocol.cptswap.pb.MessageOuterClass;

public interface CptswapController {

    void send(MessageOuterClass.Message msg);

    CompletableFuture<Unit> close();
}
