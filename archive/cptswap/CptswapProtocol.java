package tech.edgx.dee.protocol.cptswap;

import io.libp2p.core.Stream;
import io.libp2p.protocol.ProtobufProtocolHandler;
import io.libp2p.protocol.ProtocolMessageHandler;
import org.jetbrains.annotations.NotNull;
import tech.edgx.dee.protocol.cptswap.pb.MessageOuterClass;

import java.util.concurrent.CompletableFuture;

public class CptswapProtocol extends ProtobufProtocolHandler<CptswapController> {

    private final CptswapEngine engine;

    public CptswapProtocol(CptswapEngine engine) {
        super(MessageOuterClass.Message.getDefaultInstance(), Cptswap.MAX_MESSAGE_SIZE, Cptswap.MAX_MESSAGE_SIZE);
        this.engine = engine;
    }

    @NotNull
    @Override
    protected CompletableFuture<CptswapController> onStartInitiator(@NotNull Stream stream) {
        CptswapConnection conn = new CptswapConnection(stream);
        engine.addConnection(stream.remotePeerId(), stream.getConnection().remoteAddress());
        stream.pushHandler(new MessageHandler(engine));
        return CompletableFuture.completedFuture(conn);
    }

    @NotNull
    @Override
    protected CompletableFuture<CptswapController> onStartResponder(@NotNull Stream stream) {
        CptswapConnection conn = new CptswapConnection(stream);
        engine.addConnection(stream.remotePeerId(), stream.getConnection().remoteAddress());
        stream.pushHandler(new MessageHandler(engine));
        return CompletableFuture.completedFuture(conn);
    }

    class MessageHandler implements ProtocolMessageHandler<MessageOuterClass.Message> {
        private CptswapEngine engine;

        public MessageHandler(CptswapEngine engine) {
            this.engine = engine;
        }

        @Override
        public void onMessage(@NotNull Stream stream, MessageOuterClass.Message msg) {
            engine.receiveMessage(msg, stream);
        }
    }
}
