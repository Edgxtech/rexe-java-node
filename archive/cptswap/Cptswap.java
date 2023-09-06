package tech.edgx.dee.protocol.cptswap;

import com.google.protobuf.ByteString;
import io.ipfs.multihash.Multihash;
import io.libp2p.core.AddressBook;
import io.libp2p.core.Host;
import io.libp2p.core.PeerId;
import io.libp2p.core.multiformats.Multiaddr;
import io.libp2p.core.multistream.StrictProtocolBinding;
import org.peergos.AddressBookConsumer;
import org.peergos.HashedBlock;
import org.peergos.Want;
import tech.edgx.dee.model.dp.DpResult;
import tech.edgx.dee.model.dp.DpWant;
import tech.edgx.dee.protocol.cptswap.pb.MessageOuterClass;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class Cptswap extends StrictProtocolBinding<CptswapController> implements AddressBookConsumer {
    private static final Logger LOG = Logger.getLogger(Cptswap.class.getName());
    public static int MAX_MESSAGE_SIZE = 2*1024*1024;

    private final CptswapEngine engine;
    private AddressBook addrs;

    public Cptswap(CptswapEngine engine) {
        super("/ipfs/bitswap/1.2.0", new CptswapProtocol(engine));
        this.engine = engine;
    }

    public void setAddressBook(AddressBook addrs) {
        engine.setAddressBook(addrs);
        this.addrs = addrs;
    }

    public List<CompletableFuture<DpResult>> compute(List<DpWant> wants,
                                                     Host us,
                                                     Set<PeerId> peers,
                                                     boolean addToBlockstore) {
        LOG.info("Requesting wants from network, #: "+wants.size());
        if (wants.isEmpty())
            return Collections.emptyList();
        List<CompletableFuture<DpResult>> results = new ArrayList<>();
        for (DpWant w : wants) {
            if (w.cid.getType() == Multihash.Type.id)
                continue;
            CompletableFuture<DpResult> res = engine.getWant(w, addToBlockstore);
            results.add(res);
        }
        sendWants(us, peers);
        ForkJoinPool.commonPool().execute(() -> {
            while (engine.hasWants()) {
                try {Thread.sleep(5_000);} catch (InterruptedException e) {}
                sendWants(us, peers);
            }
        });
        return results;
    }

//    public CompletableFuture<HashedBlock> get(DpWant hash,
//                                           Host us,
//                                           Set<PeerId> peers,
//                                           boolean addToBlockstore) {
//        return get(List.of(hash), us, peers, addToBlockstore).get(0);
//    }

//    public List<CompletableFuture<HashedBlock>> get(List<Want> wants,
//                                                    Host us,
//                                                    Set<PeerId> peers,
//                                                    boolean addToBlockstore) {
//        LOG.info("Requesting wants from network, #: "+wants.size());
//        if (wants.isEmpty())
//            return Collections.emptyList();
//        List<CompletableFuture<HashedBlock>> results = new ArrayList<>();
//        for (Want w : wants) {
//            if (w.cid.getType() == Multihash.Type.id)
//                continue;
//            CompletableFuture<DpResult> res = engine.getWant(w, addToBlockstore);
//            results.add(res);
//        }
//        sendWants(us, peers);
//        ForkJoinPool.commonPool().execute(() -> {
//            while (engine.hasWants()) {
//                try {Thread.sleep(5_000);} catch (InterruptedException e) {}
//                sendWants(us, peers);
//            }
//        });
//        return results;
//    }

    public void sendWants(Host us, Set<PeerId> peers) {
        Set<DpWant> wants = engine.getWants();
        LOG.info("Broadcast DP wants: " + wants.size());
        Map<DpWant, PeerId> haves = engine.getHaves();
        List<MessageOuterClass.Message.Wantlist.Entry> wantsProto = wants.stream()
                .map(want -> MessageOuterClass.Message.Wantlist.Entry.newBuilder()
                        .setWantType(haves.containsKey(want) ?
                                MessageOuterClass.Message.Wantlist.WantType.Block :
                                MessageOuterClass.Message.Wantlist.WantType.Have)
                        .setBlock(ByteString.copyFrom(want.cid.toBytes()))
                        .setAuth(ByteString.copyFrom(want.auth.orElse("").getBytes()))
                        .build())
                .collect(Collectors.toList());
        // broadcast to all connected peers if none are supplied
        Set<PeerId> connected = peers.isEmpty() ? engine.getConnected() : peers;
        engine.buildAndSendMessages(wantsProto, Collections.emptyList(), Collections.emptyList(),
                msg -> connected.forEach(peer -> dialPeer(us, peer, c -> {
                    c.send(msg);
                })));
    }

    private void dialPeer(Host us, PeerId peer, Consumer<CptswapController> action) {
        Multiaddr[] addr = addrs.get(peer).join().toArray(new Multiaddr[0]);
        if (addr.length == 0)
            throw new IllegalStateException("No addresses known for peer " + peer);
        CptswapController controller = dial(us, peer, addr).getController().join();
        action.accept(controller);
    }
}
