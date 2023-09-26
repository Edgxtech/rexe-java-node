package tech.edgx.rexe.protocol.ebitswap;

import com.google.gson.Gson;
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
//import org.peergos.protocol.bitswap.BitswapController;
//import org.peergos.protocol.bitswap.pb.MessageOuterClass;
import org.peergos.util.ArrayOps;
import tech.edgx.rexe.model.dp.DpResult;
import tech.edgx.rexe.model.dp.DpWant;
import tech.edgx.rexe.protocol.ebitswap.pb.MessageOuterClass;
//import tech.edgx.rexe.util.SwapType;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class eBitSwap extends StrictProtocolBinding<eBitSwapController> implements AddressBookConsumer {
    private static final Logger LOG = Logger.getLogger(eBitSwap.class.getName());
    public static int MAX_MESSAGE_SIZE = 2*1024*1024;

    private final eBitSwapEngine engine;
    private AddressBook addrs;

    public eBitSwap(eBitSwapEngine engine) {
        /* TODO change protocol defn */
        super("/ipfs/bitswap/1.2.0", new eBitSwapProtocol(engine));
        this.engine = engine;
    }

    public eBitSwapEngine getResSwapEngine() {
        return this.engine;
    }

    public void setAddressBook(AddressBook addrs) {
        engine.setAddressBook(addrs);
        this.addrs = addrs;
    }

    public List<CompletableFuture<DpResult>> compute(List<DpWant> wants,
                                                     Host us,
                                                     Set<PeerId> peers,
                                                     boolean addToBlockstore) {
        LOG.fine("Requesting compute wants from network, #: "+wants.size());
        if (wants.isEmpty())
            return Collections.emptyList();
        List<CompletableFuture<DpResult>> results = new ArrayList<>();
        for (DpWant w : wants) {
            if (w.cid.getType() == Multihash.Type.id)
                continue;
            CompletableFuture<DpResult> res = engine.computeWant(w); //, addToBlockstore
            results.add(res);
        }
        sendDpWants(us, peers);
        ForkJoinPool.commonPool().execute(() -> {
            while (engine.hasWants()) {
                try {Thread.sleep(5_000);} catch (InterruptedException e) {}
                sendDpWants(us, peers);
            }
        });
        return results;
    }

    public List<CompletableFuture<HashedBlock>> get(List<Want> wants,
                                                    Host us,
                                                    Set<PeerId> peers,
                                                    boolean addToBlockstore) {
        LOG.fine("Requesting get wants from network, #: "+wants.size());
        if (wants.isEmpty())
            return Collections.emptyList();
        List<CompletableFuture<HashedBlock>> results = new ArrayList<>();
        for (Want w : wants) {
            if (w.cid.getType() == Multihash.Type.id)
                continue;
            CompletableFuture<HashedBlock> res = engine.getWant(w, addToBlockstore);
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

    public void sendWants(Host us, Set<PeerId> peers) {
        Set<Want> wants = engine.getWants();
        LOG.fine("Broadcast wants, #: " + wants.size());
        Map<Want, PeerId> haves = engine.getHaves();
        List<MessageOuterClass.Message.Wantlist.Entry> wantsProto = wants.stream()
                .map(want -> MessageOuterClass.Message.Wantlist.Entry.newBuilder()
                        .setWantType(haves.containsKey(want) ?
                                MessageOuterClass.Message.Wantlist.WantType.Block :
                                MessageOuterClass.Message.Wantlist.WantType.Have)
                        .setBlock(ByteString.copyFrom(want.cid.toBytes()))
                        .setAuth(ByteString.copyFrom(ArrayOps.hexToBytes(want.authHex.orElse(""))))
                        .build())
                .collect(Collectors.toList());
        Set<PeerId> connected = peers.isEmpty() ? engine.getConnected() : peers;
        LOG.fine("Sending wants: "+new Gson().toJson(wantsProto)+", Broadcasting to: "+new Gson().toJson(connected));
        engine.buildAndSendMessages(wantsProto,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                msg -> connected.forEach(peer -> dialPeer(us, peer, c -> {
                    c.send(msg);
                })));
    }

    public void sendDpWants(Host us, Set<PeerId> peers) {
        Set<DpWant> wants = engine.getDpWants();
        LOG.info("Broadcast DP wants, #: " + wants.size());
        List<MessageOuterClass.Message.Wantlist.Entry> wantsProto = wants.stream()
                .map(want -> MessageOuterClass.Message.Wantlist.Entry.newBuilder()
                        .setWantType(MessageOuterClass.Message.Wantlist.WantType.Dp)
                        .setBlock(ByteString.copyFrom(want.cid.toBytes()))
                        .setAuth(ByteString.copyFrom(want.auth.orElse("").getBytes()))
                        .setFunctionName(ByteString.copyFrom(want.functionName.getBytes()))
                        .addAllParams(want.params.isPresent() ? Arrays.stream(want.params.get()).map(p -> ByteString.copyFrom(p.toString().getBytes())).collect(Collectors.toList()) : new ArrayList<>())
                        .build())
                .collect(Collectors.toList());
        Set<PeerId> connected = peers.isEmpty() ? engine.getConnected() : peers;
        LOG.fine("Sending DP wants: "+new Gson().toJson(wantsProto)+", Broadcasting to: "+new Gson().toJson(connected));
        engine.buildAndSendMessages(wantsProto, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                msg -> connected.forEach(peer -> dialPeer(us, peer, c -> {
                    c.send(msg);
                })));
    }

    private void dialPeer(Host us, PeerId peer, Consumer<eBitSwapController> action) {
        Multiaddr[] addr = addrs.get(peer).join().toArray(new Multiaddr[0]);
        if (addr.length == 0)
            throw new IllegalStateException("No addresses known for peer " + peer);
        eBitSwapController controller = dial(us, peer, addr).getController().join();
        action.accept(controller);
    }
}
