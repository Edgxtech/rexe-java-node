package tech.edgx.dee.protocol.dpswap;

import io.libp2p.core.Stream;
import io.libp2p.protocol.ProtobufProtocolHandler;
import io.libp2p.protocol.ProtocolMessageHandler;
import org.jetbrains.annotations.NotNull;
import tech.edgx.dee.protocol.dpswap.pb.MessageOuterClass;

import java.util.concurrent.CompletableFuture;

public class DpswapProtocol extends ProtobufProtocolHandler<DpswapController> {

    private final DpswapEngine engine;

    public DpswapProtocol(DpswapEngine engine) {
        super(MessageOuterClass.Message.getDefaultInstance(), Dpswap.MAX_MESSAGE_SIZE, Dpswap.MAX_MESSAGE_SIZE);
        this.engine = engine;
    }

    @NotNull
    @Override
    protected CompletableFuture<DpswapController> onStartInitiator(@NotNull Stream stream) {
        DpswapConnection conn = new DpswapConnection(stream);
        engine.addConnection(stream.remotePeerId(), stream.getConnection().remoteAddress());
        stream.pushHandler(new MessageHandler(engine));
        return CompletableFuture.completedFuture(conn);
    }

    @NotNull
    @Override
    protected CompletableFuture<DpswapController> onStartResponder(@NotNull Stream stream) {
        DpswapConnection conn = new DpswapConnection(stream);
        engine.addConnection(stream.remotePeerId(), stream.getConnection().remoteAddress());
        stream.pushHandler(new MessageHandler(engine));
        return CompletableFuture.completedFuture(conn);
    }

    class MessageHandler implements ProtocolMessageHandler<MessageOuterClass.Message> {
        private DpswapEngine engine;

        public MessageHandler(DpswapEngine engine) {
            this.engine = engine;
        }

        @Override
        public void onMessage(@NotNull Stream stream, MessageOuterClass.Message msg) {
            engine.receiveMessage(msg, stream);
        }
    }
}
