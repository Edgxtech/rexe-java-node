package tech.edgx.rexe.protocol.ebitswap;

import io.libp2p.core.Stream;
import io.libp2p.protocol.ProtobufProtocolHandler;
import io.libp2p.protocol.ProtocolMessageHandler;
import org.jetbrains.annotations.NotNull;
import tech.edgx.rexe.protocol.ebitswap.pb.MessageOuterClass;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class eBitSwapProtocol extends ProtobufProtocolHandler<eBitSwapController> {

    private static final Logger LOG = Logger.getLogger(eBitSwapProtocol.class.getName());

    private final eBitSwapEngine engine;

    public eBitSwapProtocol(eBitSwapEngine engine) {
        super(MessageOuterClass.Message.getDefaultInstance(), eBitSwap.MAX_MESSAGE_SIZE, eBitSwap.MAX_MESSAGE_SIZE);
        this.engine = engine;
    }

    @NotNull
    @Override
    protected CompletableFuture<eBitSwapController> onStartInitiator(@NotNull Stream stream) {
        eBitSwapConnection conn = new eBitSwapConnection(stream);
        LOG.fine("onStart eBitSwap Initiator Protocol Connection: "+conn);
        engine.addConnection(stream.remotePeerId(), stream.getConnection().remoteAddress());
        stream.pushHandler(new MessageHandler(engine));
        return CompletableFuture.completedFuture(conn);
    }

    @NotNull
    @Override
    protected CompletableFuture<eBitSwapController> onStartResponder(@NotNull Stream stream) {
        eBitSwapConnection conn = new eBitSwapConnection(stream);
        LOG.fine("onStart eBitSwap Responder Protocol Connection: "+conn);
        engine.addConnection(stream.remotePeerId(), stream.getConnection().remoteAddress());
        stream.pushHandler(new MessageHandler(engine));
        return CompletableFuture.completedFuture(conn);
    }

    class MessageHandler implements ProtocolMessageHandler<MessageOuterClass.Message> {
        private eBitSwapEngine engine;

        public MessageHandler(eBitSwapEngine engine) {
            this.engine = engine;
        }

        @Override
        public void onMessage(@NotNull Stream stream, MessageOuterClass.Message msg) {
            engine.receiveMessage(msg, stream);
        }
    }
}
