package org.peergos;

import io.ipfs.multiaddr.*;
import io.ipfs.multihash.Multihash;
import io.libp2p.core.*;
import io.libp2p.core.crypto.*;
import io.libp2p.core.dsl.*;
import io.libp2p.core.multiformats.*;
import io.libp2p.core.multistream.*;
import io.libp2p.core.mux.*;
import io.libp2p.crypto.keys.*;
import io.libp2p.protocol.*;
import io.libp2p.security.noise.*;
import io.libp2p.security.tls.*;
import io.libp2p.transport.tcp.*;
import io.libp2p.core.crypto.KeyKt;
import org.peergos.blockstore.*;
import org.peergos.protocol.autonat.*;
import org.peergos.protocol.bitswap.*;
import org.peergos.protocol.circuit.*;
import org.peergos.protocol.dht.*;
import tech.edgx.rexe.protocol.ebitswap.eBitSwap;
import tech.edgx.rexe.protocol.ebitswap.eBitSwapEngine;

import java.util.*;
import java.util.stream.*;
import java.util.logging.Logger;

public class HostBuilder {
    private static final Logger LOG = Logger.getLogger(HostBuilder.class.getName());

    private PrivKey privKey;
    private PeerId peerId;
    private List<String> listenAddrs = new ArrayList<>();
    private List<ProtocolBinding> protocols = new ArrayList<>();
    private List<StreamMuxerProtocol> muxers = new ArrayList<>();

    public HostBuilder() {
    }

    public PrivKey getPrivateKey() {
        return privKey;
    }

    public PeerId getPeerId() {
        return peerId;
    }

    public List<ProtocolBinding> getProtocols() {
        return this.protocols;
    }

    public Optional<Kademlia> getWanDht() {
        return protocols.stream()
                .filter(p -> p instanceof Kademlia && p.getProtocolDescriptor().getAnnounceProtocols().contains("/ipfs/kad/1.0.0"))
                .map(p -> (Kademlia)p)
                .findFirst();
    }

    // DEPRECATE THIS, SWITCH TO COMPUTESWAP
    public Optional<Bitswap> getBitswap() {
        return protocols.stream()
                .filter(p -> p instanceof Bitswap)
                .map(p -> (Bitswap)p)
                .findFirst();
    }

    public Optional<eBitSwap> getResSwap() {
        return protocols.stream()
                .filter(p -> p instanceof eBitSwap)
                .map(p -> (eBitSwap)p)
                .findFirst();
    }

    public Optional<CircuitHopProtocol.Binding> getRelayHop() {
        return protocols.stream()
                .filter(p -> p instanceof CircuitHopProtocol.Binding)
                .map(p -> (CircuitHopProtocol.Binding)p)
                .findFirst();
    }

    public HostBuilder addMuxers(List<StreamMuxerProtocol> muxers) {
        this.muxers.addAll(muxers);
        return this;
    }

    public HostBuilder addProtocols(List<ProtocolBinding> protocols) {
        this.protocols.addAll(protocols);
        return this;
    }

    public HostBuilder addProtocol(ProtocolBinding protocols) {
        this.protocols.add(protocols);
        return this;
    }

    public HostBuilder listen(List<MultiAddress> listenAddrs) {
        this.listenAddrs.addAll(listenAddrs.stream().map(MultiAddress::toString).collect(Collectors.toList()));
        return this;
    }

    public HostBuilder generateIdentity() {
        return setPrivKey(Ed25519Kt.generateEd25519KeyPair().getFirst());
    }

    public HostBuilder setIdentity(byte[] privKey) {
        return setPrivKey(KeyKt.unmarshalPrivateKey(privKey));
    }



    public HostBuilder setPrivKey(PrivKey privKey) {
        this.privKey = privKey;
        this.peerId = PeerId.fromPubKey(privKey.publicKey());
        return this;
    }

    public static HostBuilder create(int listenPort,
                                     ProviderStore providers,
                                     RecordStore records,
                                     Blockstore blocks,
                                     BlockRequestAuthoriser authoriser) {
        HostBuilder builder = new HostBuilder()
                .generateIdentity()
                .listen(List.of(new MultiAddress("/ip4/0.0.0.0/tcp/" + listenPort)));
        Multihash ourPeerId = Multihash.deserialize(builder.peerId.getBytes());
        Kademlia dht = new Kademlia(new KademliaEngine(ourPeerId, providers, records, blocks), false);
        CircuitStopProtocol.Binding stop = new CircuitStopProtocol.Binding();
        CircuitHopProtocol.RelayManager relayManager = CircuitHopProtocol.RelayManager.limitTo(builder.privKey, ourPeerId, 5);
        return builder.addProtocols(List.of(
                new Ping(),
                new AutonatProtocol.Binding(),
                new CircuitHopProtocol.Binding(relayManager, stop),
                // REPLACE Bitswap protocol with ResSwap which is bitswap extended with distributed computation capy
                // FOR NOW USING THE SAME BLOCK STORE TO STORE DPs as if they are blocks
                //new Bitswap(new BitswapEngine(blocks, authoriser)),
                new eBitSwap(new eBitSwapEngine(blocks, authoriser)),
                dht));
    }

    public static Host build(int listenPort,
                             List<ProtocolBinding> protocols) {
        return new HostBuilder()
                .generateIdentity()
                .listen(List.of(new MultiAddress("/ip4/0.0.0.0/tcp/" + listenPort)))
                .addProtocols(protocols)
                .build();
    }

    public Host build() {
        if (muxers.isEmpty())
            muxers.addAll(List.of(StreamMuxerProtocol.getYamux(), StreamMuxerProtocol.getMplex()));
        return build(privKey, listenAddrs, protocols, muxers);
    }

    public static Host build(PrivKey privKey,
                             List<String> listenAddrs,
                             List<ProtocolBinding> protocols,
                             List<StreamMuxerProtocol> muxers) {
        Host host = BuilderJKt.hostJ(Builder.Defaults.None, b -> {
            b.getIdentity().setFactory(() -> privKey);
            b.getTransports().add(TcpTransport::new);
            b.getSecureChannels().add(NoiseXXSecureChannel::new);
            b.getSecureChannels().add(TlsSecureChannel::new);

            b.getMuxers().addAll(muxers);
            RamAddressBook addrs = new RamAddressBook();
            b.getAddressBook().setImpl(addrs);
            // Uncomment to add mux debug logging
//            b.getDebug().getMuxFramesHandler().addLogger(LogLevel.INFO, "MUX");

            for (ProtocolBinding<?> protocol : protocols) {
                b.getProtocols().add(protocol);
                if (protocol instanceof AddressBookConsumer)
                    ((AddressBookConsumer) protocol).setAddressBook(addrs);
            }

            // Send an identify req on all new incoming connections
            b.getConnectionHandlers().add(connection -> {
                PeerId remotePeer = connection.secureSession().getRemoteId();
                Multiaddr remote = connection.remoteAddress().withP2P(remotePeer);
                addrs.addAddrs(remotePeer, 0, remote);
                if (connection.isInitiator())
                    return;
                StreamPromise<IdentifyController> stream = connection.muxerSession()
                        .createStream(new IdentifyBinding(new IdentifyProtocol()));
                stream.getController()
                        .thenCompose(IdentifyController::id)
                        .thenApply(remoteId -> addrs.addAddrs(remotePeer, 0, remoteId.getListenAddrsList()
                                .stream()
                                .map(bytes -> Multiaddr.deserialize(bytes.toByteArray()))
                                .toArray(Multiaddr[]::new)));
            });

            for (String listenAddr : listenAddrs) {
                b.getNetwork().listen(listenAddr);
            }

//            b.getConnectionHandlers().add(conn -> System.out.println(conn.localAddress() +
//                    " received connection from " + conn.remoteAddress() +
//                    " on transport " + conn.transport()));
        });
        for (ProtocolBinding protocol : protocols) {
            if (protocol instanceof HostConsumer)
                ((HostConsumer)protocol).setHost(host);
        }
        return host;
    }
}
