package org.peergos;

import com.google.gson.Gson;
import io.ipfs.multiaddr.*;
import io.ipfs.multihash.Multihash;
import io.libp2p.core.*;
import io.libp2p.core.multistream.*;
import io.libp2p.protocol.*;
import org.peergos.blockstore.*;
import org.peergos.blockstore.s3.S3Blockstore;
import org.peergos.config.*;
import org.peergos.protocol.*;
import org.peergos.protocol.autonat.*;
import org.peergos.protocol.circuit.*;
import org.peergos.protocol.dht.*;
import org.peergos.protocol.http.*;
import tech.edgx.rexe.model.dp.DpResult;
import tech.edgx.rexe.model.dp.DpWant;
import tech.edgx.rexe.protocol.ebitswap.eBitSwap;
import tech.edgx.rexe.protocol.ebitswap.eBitSwapEngine;
import tech.edgx.rexe.service.DPExecutionService;
import tech.edgx.rexe.service.ResourceService;
import tech.edgx.rexe.service.ResourceServiceImpl;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;
import java.util.stream.*;

public class EmbeddedIpfs {
    private static final Logger LOG = Logger.getLogger(EmbeddedIpfs.class.getName());

    public final Host node;
    public final ProvidingBlockstore blockstore;
    public final DatabaseRecordStore records;

    public final Kademlia dht;
    public final Optional<HttpProtocol.Binding> p2pHttp;
    private final List<MultiAddress> bootstrap;
    private final PeriodicBlockProvider blockProvider;

    private final DPExecutionService DPExecutionService;
    private final ResourceService resourceService;
    public final eBitSwap eBitSwap;

    public EmbeddedIpfs(Host node,
                        ProvidingBlockstore blockstore,
                        DatabaseRecordStore records,
                        Kademlia dht,
                        eBitSwap eBitSwap,
                        Optional<HttpProtocol.Binding> p2pHttp,
                        List<MultiAddress> bootstrap) {
        this.node = node;
        this.blockstore = blockstore;
        this.records = records;
        this.dht = dht;
        this.eBitSwap = eBitSwap;
        this.p2pHttp = p2pHttp;
        this.bootstrap = bootstrap;
        this.blockProvider = new PeriodicBlockProvider(22 * 3600_000L,
                () -> blockstore.refs().join().stream(), node, dht, blockstore.toPublish);

        this.DPExecutionService = new DPExecutionService();
        this.resourceService = new ResourceServiceImpl(node, eBitSwap, dht);
    }

    public List<HashedBlock> getBlocks(List<Want> wants, Set<PeerId> peers, boolean addToLocal) {
        List<HashedBlock> blocksFound = new ArrayList<>();

        List<Want> local = new ArrayList<>();
        List<Want> remote = new ArrayList<>();

        for (Want w : wants) {
            if (blockstore.has(w.cid).join()) {
                LOG.fine("Has block locally: " + w.cid.bareMultihash().toBase58());
                local.add(w);
            }
            else
                remote.add(w);
        }
        local.stream()
                .map(w -> new HashedBlock(w.cid, blockstore.get(w.cid).join().get()))
                .forEach(blocksFound::add);
        if (remote.isEmpty())
            return blocksFound;
        return java.util.stream.Stream.concat(
                        blocksFound.stream(),
                        resourceService.get(remote, peers, addToLocal).stream())
                .collect(Collectors.toList());
    }

    public List<DpResult> computeDp(List<DpWant> wants, Set<PeerId> peers, boolean addToLocal) {
        List<DpResult> resultsComputed = new ArrayList<>();
        List<DpWant> local = new ArrayList<>();
        List<DpWant> remote = new ArrayList<>();
        for (DpWant w : wants) {
            if (blockstore.has(w.cid).join())
                local.add(w);
            else
                remote.add(w);
        }
        LOG.fine("Computing DPs locally, #: "+local.size()+", Computing DPs remote, #: "+remote.size());
        for (DpWant w : local) {
            LOG.fine("Executing: "+w.cid.toString()+", "+w.functionName);
            try {
                DpResult dpResult = DPExecutionService.runDp(w.cid, blockstore.get(w.cid).join().get(), w.functionName, w.params, w.args);
                resultsComputed.add(dpResult);
            } catch(Exception e) {
                e.printStackTrace();
                LOG.fine("Failed to execute DP: "+w.cid.toString());
                return null;
            }
        }
        if (remote.isEmpty())
            return resultsComputed;

        return java.util.stream.Stream.concat(
                        resultsComputed.stream(),
                        resourceService.compute(remote, peers, addToLocal).stream())
                .collect(Collectors.toList());
    }

    public void start() {
        LOG.info("Starting IPFS...");
        LOG.info("Listen addresses: "+new Gson().toJson(node.listenAddresses()));
        node.start().join();
        IdentifyBuilder.addIdentifyProtocol(node);
        LOG.info("Node started and listening on " + node.listenAddresses());
        LOG.info("Bootstrapping IPFS routing table");
        int connections = dht.bootstrapRoutingTable(node, bootstrap, addr -> !addr.contains("/wss/"));
        LOG.info("Bootstrapping IPFS kademlia");
        dht.bootstrap(node);

        blockProvider.start();
    }

    public CompletableFuture<Void> stop() throws Exception {
        records.close();
        blockProvider.stop();
        return node.stop();
    }

    public static Blockstore buildBlockStore(Config config, Path ipfsPath) {
        Blockstore blocks = null;
        if (config.datastore.blockMount.prefix.equals("flatfs.datastore")) {
            blocks = new FileBlockstore(ipfsPath);
        }else if (config.datastore.blockMount.prefix.equals("s3.datastore")) {
            blocks = new S3Blockstore(config.datastore.blockMount.getParams());
        } else {
            throw new IllegalStateException("Unrecognized datastore prefix: " + config.datastore.blockMount.prefix);
        }
        Blockstore blockStore;
        if (config.datastore.filter.type == FilterType.BLOOM) {
            blockStore = FilteredBlockstore.bloomBased(blocks, config.datastore.filter.falsePositiveRate);
        } else if(config.datastore.filter.type == FilterType.INFINI) {
            blockStore = FilteredBlockstore.infiniBased(blocks, config.datastore.filter.falsePositiveRate);
        } else if(config.datastore.filter.type == FilterType.NONE) {
            blockStore = blocks;
        } else {
            throw new IllegalStateException("Unhandled filter type: " + config.datastore.filter.type);
        }
        return config.datastore.allowedCodecs.codecs.isEmpty() ?
                blockStore : new TypeLimitedBlockstore(blockStore, config.datastore.allowedCodecs.codecs);
    }

    public static EmbeddedIpfs build(Path ipfsPath,
                                     Blockstore blocks,
                                     List<MultiAddress> swarmAddresses,
                                     List<MultiAddress> bootstrap,
                                     IdentitySection identity,
                                     BlockRequestAuthoriser authoriser,
                                     Optional<HttpProtocol.HttpRequestProcessor> handler) {
        ProvidingBlockstore blockstore = new ProvidingBlockstore(blocks);
        Path datastorePath = ipfsPath.resolve("datastore").resolve("h2.datastore");
        DatabaseRecordStore records = new DatabaseRecordStore(datastorePath.toString());
        ProviderStore providers = new RamProviderStore();

        HostBuilder builder = new HostBuilder().setIdentity(identity.privKeyProtobuf).listen(swarmAddresses);
        if (! builder.getPeerId().equals(identity.peerId)) {
            throw new IllegalStateException("PeerId invalid");
        }
        Multihash ourPeerId = Multihash.deserialize(builder.getPeerId().getBytes());

        Kademlia dht = new Kademlia(new KademliaEngine(ourPeerId, providers, records, blockstore), false);
        CircuitStopProtocol.Binding stop = new CircuitStopProtocol.Binding();
        CircuitHopProtocol.RelayManager relayManager = CircuitHopProtocol.RelayManager.limitTo(builder.getPrivateKey(), ourPeerId, 5);
        eBitSwap eBitSwap = new eBitSwap(new eBitSwapEngine(blockstore, authoriser));
        Optional<HttpProtocol.Binding> httpHandler = handler.map(HttpProtocol.Binding::new);

        List<ProtocolBinding> protocols = new ArrayList<>();
        protocols.add(new Ping());
        protocols.add(new AutonatProtocol.Binding());
        protocols.add(new CircuitHopProtocol.Binding(relayManager, stop));
        protocols.add(eBitSwap);

        protocols.add(dht);
        httpHandler.ifPresent(protocols::add);

        Host node = builder.addProtocols(protocols).build();

        return new EmbeddedIpfs(node, blockstore, records, dht, eBitSwap, httpHandler, bootstrap);
    }
}
