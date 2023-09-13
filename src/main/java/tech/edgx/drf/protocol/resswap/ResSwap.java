package tech.edgx.drf.protocol.resswap;

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
import tech.edgx.drf.model.dp.DpResult;
import tech.edgx.drf.model.dp.DpWant;
import tech.edgx.drf.protocol.resswap.pb.MessageOuterClass;
//import tech.edgx.drf.util.SwapType;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Collectors;

// Should I call this Netswap?
//  Swapping data and computational resources; wants, haves, blocks, compute results
// Or call integration, Resswap, ResourceSwap, Reswap, ReSwap - Resource Swap
// PeerSwap
// Since integration now is the mechanism for exchanging Resources as I have defined.
//    How to add streaming channels????
//

public class ResSwap extends StrictProtocolBinding<ResSwapController> implements AddressBookConsumer {
    private static final Logger LOG = Logger.getLogger(ResSwap.class.getName());
    public static int MAX_MESSAGE_SIZE = 2*1024*1024;

    private final ResSwapEngine engine;
    private AddressBook addrs;

    public ResSwap(ResSwapEngine engine) {
        // TODO, how can I edit this and have my other nodes connect?
        // IF RUNNING SOME TESTS that bootstrap off the internet, MUST USE /ipfs/bitswap/1.2.0
        //super("/drf/resswap/1.0.0", new ResSwapProtocol(engine));
        super("/ipfs/bitswap/1.2.0", new ResSwapProtocol(engine));
        this.engine = engine;
    }

    public ResSwapEngine getResSwapEngine() {
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
        LOG.info("Requesting compute wants from network, #: "+wants.size());
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

//    public CompletableFuture<HashedBlock> get(DpWant hash,
//                                           Host us,
//                                           Set<PeerId> peers,
//                                           boolean addToBlockstore) {
//        return get(List.of(hash), us, peers, addToBlockstore).get(0);
//    }

    public List<CompletableFuture<HashedBlock>> get(List<Want> wants,
                                                    Host us,
                                                    Set<PeerId> peers,
                                                    boolean addToBlockstore) {
        LOG.info("Requesting get wants from network, #: "+wants.size());
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

//    public void sendWants(Host us, Set<PeerId> peers) {
//        Set<Want> wants = engine.getWants();
//        LOG.info("Broadcast wants: " + wants.size());
//        Map<Want, PeerId> haves = engine.getHaves();
//        List<org.peergos.protocol.bitswap.pb.MessageOuterClass.Message.Wantlist.Entry> wantsProto = wants.stream()
//                .map(want -> org.peergos.protocol.bitswap.pb.MessageOuterClass.Message.Wantlist.Entry.newBuilder()
//                        //.setWantType(
//                        .setWantType(haves.containsKey(want) ?
//                                org.peergos.protocol.bitswap.pb.MessageOuterClass.Message.Wantlist.WantType.Block :
//                                MessageOuterClass.Message.Wantlist.WantType.Have)
//                        .setBlock(ByteString.copyFrom(want.cid.toBytes()))
//                        .setAuth(ByteString.copyFrom(want.auth.orElse("").getBytes()))
//                        .build())
//                .collect(Collectors.toList());
//        // broadcast to all connected peers if none are supplied
//        Set<PeerId> connected = peers.isEmpty() ? engine.getConnected() : peers;
//        engine.buildAndSendMessages(wantsProto, Collections.emptyList(), Collections.emptyList(),
//                msg -> connected.forEach(peer -> dialPeer(us, peer, c -> {
//                    c.send(msg);
//                })));
//    }

    public void sendWants(Host us, Set<PeerId> peers) {
        Set<Want> wants = engine.getWants();
        LOG.info("Broadcast wants Reswap: " + wants.size());
        //System.out.println("Broadcast wants: " + wants.size());
        Map<Want, PeerId> haves = engine.getHaves();
        List<MessageOuterClass.Message.Wantlist.Entry> wantsProto = wants.stream()
                .map(want -> MessageOuterClass.Message.Wantlist.Entry.newBuilder()
                        .setWantType(haves.containsKey(want) ?
                                MessageOuterClass.Message.Wantlist.WantType.Block :
                                MessageOuterClass.Message.Wantlist.WantType.Have)
                        .setBlock(ByteString.copyFrom(want.cid.toBytes()))
                        //.setAuth(ByteString.copyFrom(want.auth.orElse("").getBytes()))
                        .setAuth(ByteString.copyFrom(ArrayOps.hexToBytes(want.authHex.orElse(""))))
                        .build())
                .collect(Collectors.toList());
        // broadcast to all connected peers if none are supplied
        Set<PeerId> connected = peers.isEmpty() ? engine.getConnected() : peers;
        LOG.info("Engine connected peers: "+new Gson().toJson(engine.getConnected().stream().map(p -> p.toBase58()).collect(Collectors.toList()))+", Provided peers: "+new Gson().toJson(peers.stream().map(p -> p.toBase58()).collect(Collectors.toList())));
        //System.out.println("Engine connected peers: "+new Gson().toJson(engine.getConnected())+", Provided peers: "+new Gson().toJson(peers));
        LOG.info("Broad casting to: "+new Gson().toJson(connected));

        LOG.info("Requesting: "+new Gson().toJson(wantsProto));

        // DIAL peer and perform the specified action which is to send messages,
        //  namely the Wants list wrapped in the MessageOuterClass
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
        LOG.info("Broadcast DP wants: " + wants.size());

        //Map<DpWant, PeerId> haves = engine.getHaves();
        List<MessageOuterClass.Message.Wantlist.Entry> wantsProto = wants.stream()
                .map(want -> MessageOuterClass.Message.Wantlist.Entry.newBuilder()
//                        .setWantType(haves.containsKey(want) ?
//                                MessageOuterClass.Message.Wantlist.WantType.Block :
//                                MessageOuterClass.Message.Wantlist.WantType.Have)
                        .setWantType(MessageOuterClass.Message.Wantlist.WantType.Dp)
//                        .setWantType(swapType.equals(SwapType.block) ?
//                                haves.containsKey(want) ?
//                                        tech.edgx.drf.protocol.resswap.pb.MessageOuterClass.Message.Wantlist.WantType.Block :
//                                        tech.edgx.drf.protocol.resswap.pb.MessageOuterClass.Message.Wantlist.WantType.Have :
//                                tech.edgx.drf.protocol.resswap.pb.MessageOuterClass.Message.Wantlist.WantType.Dp)
                        .setBlock(ByteString.copyFrom(want.cid.toBytes()))
                        .setAuth(ByteString.copyFrom(want.auth.orElse("").getBytes()))
                        .setFunctionName(ByteString.copyFrom(want.functionName.getBytes()))
                        //ByteString.copyFrom(want.params.orElse("").stream().reduce(p -> new String(p).getBytes()).
                        //.setParams(ByteString.copyFrom("".getBytes()))
                        //.setParams(ByteAr.copyFrom(want.params.get().))
                        .addAllParams(want.params.isPresent() ? Arrays.stream(want.params.get()).map(p -> ByteString.copyFrom(p.toString().getBytes())).collect(Collectors.toList()) : new ArrayList<>())
                        .build())
                .collect(Collectors.toList());
        LOG.info("Sending wants: "+new Gson().toJson(wantsProto));
        // broadcast to all connected peers if none are supplied
        Set<PeerId> connected = peers.isEmpty() ? engine.getConnected() : peers;
        engine.buildAndSendMessages(wantsProto, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                msg -> connected.forEach(peer -> dialPeer(us, peer, c -> {
                    c.send(msg);
                })));
    }

    private void dialPeer(Host us, PeerId peer, Consumer<ResSwapController> action) {
        Multiaddr[] addr = addrs.get(peer).join().toArray(new Multiaddr[0]);
        if (addr.length == 0)
            throw new IllegalStateException("No addresses known for peer " + peer);
        ResSwapController controller = dial(us, peer, addr).getController().join();
        action.accept(controller);
    }

    // Ref from bitswap test
//    private static long findAndDialPeer(Multihash toFind, Kademlia dht1, Host node1) {
//        long t1 = System.currentTimeMillis();
//        List<PeerAddresses> closestPeers = dht1.findClosestPeers(toFind, 1, node1);
//        long t2 = System.currentTimeMillis();
//        Optional<PeerAddresses> matching = closestPeers.stream()
//                .filter(p -> p.peerId.equals(toFind))
//                .findFirst();
//        if (matching.isEmpty())
//            throw new IllegalStateException("Couldn't find node2 from kubo!");
//        PeerAddresses peer = matching.get();
//        Multiaddr[] addrs = peer.getPublicAddresses().stream().map(a -> Multiaddr.fromString(a.toString())).toArray(Multiaddr[]::new);
//        dht1.dial(node1, PeerId.fromBase58(peer.peerId.toBase58()), addrs)
//                .getController().join().closerPeers(toFind).join();
//        System.out.println("Peer lookup took " + (t2-t1) + "ms");
//        return t2 - t1;
//    }

}
