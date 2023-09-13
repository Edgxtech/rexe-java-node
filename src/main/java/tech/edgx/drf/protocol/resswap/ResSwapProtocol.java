package tech.edgx.drf.protocol.resswap;

import io.libp2p.core.Stream;
import io.libp2p.protocol.ProtobufProtocolHandler;
import io.libp2p.protocol.ProtocolMessageHandler;
import org.jetbrains.annotations.NotNull;
import tech.edgx.drf.protocol.resswap.pb.MessageOuterClass;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class ResSwapProtocol extends ProtobufProtocolHandler<ResSwapController> {

    private static final Logger LOG = Logger.getLogger(ResSwapProtocol.class.getName());

    private final ResSwapEngine engine;

    public ResSwapProtocol(ResSwapEngine engine) {
        super(MessageOuterClass.Message.getDefaultInstance(), ResSwap.MAX_MESSAGE_SIZE, ResSwap.MAX_MESSAGE_SIZE);
        this.engine = engine;
    }

    @NotNull
    @Override
    protected CompletableFuture<ResSwapController> onStartInitiator(@NotNull Stream stream) {
        ResSwapConnection conn = new ResSwapConnection(stream);
        // NOTE: This only fires when bootstrapping off the public ipfs net
        // Need to find a way to create own private IPFS net, kubo allows for swarm.key and private net
        System.out.println("onStart ResSwap Initiator Protocol Connection: "+conn);
        engine.addConnection(stream.remotePeerId(), stream.getConnection().remoteAddress());
        stream.pushHandler(new MessageHandler(engine));
        return CompletableFuture.completedFuture(conn);
    }

    @NotNull
    @Override
    protected CompletableFuture<ResSwapController> onStartResponder(@NotNull Stream stream) {
        ResSwapConnection conn = new ResSwapConnection(stream);
        System.out.println("onStart ResSwap Responder Protocol Connection: "+conn);
        engine.addConnection(stream.remotePeerId(), stream.getConnection().remoteAddress());
        stream.pushHandler(new MessageHandler(engine));
        return CompletableFuture.completedFuture(conn);
    }

    class MessageHandler implements ProtocolMessageHandler<MessageOuterClass.Message> {
        private ResSwapEngine engine;

        public MessageHandler(ResSwapEngine engine) {
            this.engine = engine;
        }

        @Override
        public void onMessage(@NotNull Stream stream, MessageOuterClass.Message msg) {
            engine.receiveMessage(msg, stream);
        }
    }
}
