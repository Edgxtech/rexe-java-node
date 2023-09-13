package org.peergos.protocol.bitswap;

import com.google.gson.Gson;
import io.libp2p.core.*;
import io.libp2p.protocol.*;
import org.jetbrains.annotations.*;
import org.peergos.APIService;
import org.peergos.protocol.bitswap.pb.*;

import java.util.concurrent.*;
import java.util.logging.Logger;

public class BitswapProtocol extends ProtobufProtocolHandler<BitswapController> {

    private static final Logger LOG = Logger.getLogger(BitswapProtocol.class.getName());

    private final BitswapEngine engine;

    public BitswapProtocol(BitswapEngine engine) {
        super(MessageOuterClass.Message.getDefaultInstance(), Bitswap.MAX_MESSAGE_SIZE, Bitswap.MAX_MESSAGE_SIZE);
        System.out.println("Constructing bitswap protocol");
        this.engine = engine;
    }

    @NotNull
    @Override
    protected CompletableFuture<BitswapController> onStartInitiator(@NotNull Stream stream) {
        BitswapConnection conn = new BitswapConnection(stream);
        System.out.println("onStart BitswapSwap Initiator Protocol Connection: "+conn);
        engine.addConnection(stream.remotePeerId(), stream.getConnection().remoteAddress());
        stream.pushHandler(new MessageHandler(engine));
        return CompletableFuture.completedFuture(conn);
    }

    @NotNull
    @Override
    protected CompletableFuture<BitswapController> onStartResponder(@NotNull Stream stream) {
        BitswapConnection conn = new BitswapConnection(stream);
        System.out.println("onStart BitSwap Responder Protocol Connection: "+conn);
        engine.addConnection(stream.remotePeerId(), stream.getConnection().remoteAddress());
        stream.pushHandler(new MessageHandler(engine));
        return CompletableFuture.completedFuture(conn);
    }

    class MessageHandler implements ProtocolMessageHandler<MessageOuterClass.Message> {
        private BitswapEngine engine;

        public MessageHandler(BitswapEngine engine) {
            this.engine = engine;
        }

        @Override
        public void onMessage(@NotNull Stream stream, MessageOuterClass.Message msg) {
            engine.receiveMessage(msg, stream);
        }
    }
}
