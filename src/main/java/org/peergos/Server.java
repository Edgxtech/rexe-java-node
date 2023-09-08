package org.peergos;

import com.sun.net.httpserver.HttpServer;
import io.ipfs.multiaddr.MultiAddress;
import io.ipfs.multihash.Multihash;
import io.libp2p.core.*;
import io.libp2p.protocol.Ping;
import org.peergos.blockstore.*;
import org.peergos.config.*;
import org.peergos.net.APIHandler;
import org.peergos.protocol.autonat.AutonatProtocol;
import org.peergos.protocol.circuit.CircuitHopProtocol;
import org.peergos.protocol.circuit.CircuitStopProtocol;
import org.peergos.protocol.dht.*;
import org.peergos.util.Logging;
import tech.edgx.dee.protocol.resswap.ResSwap;
import tech.edgx.dee.protocol.resswap.ResSwapEngine;
import tech.edgx.dee.service.ResourceServiceImpl;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server {

    private static final Logger LOG = Logger.getLogger(Server.class.getName());

    private Blockstore buildBlockStore(Config config, Path blocksPath) {
        FileBlockstore blocks = new FileBlockstore(blocksPath);
        Blockstore blockStore = null;
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

    // CONCEPT IS TO EXTEND IPFS-JAVA-NODE
    //   - Add PKI, and user id
    //   - Add Distributed execution environment

    // ALTERNATIVE is to extend PEERGOS
    //   - Add Distributed execution environment
    //   - Remove a bunch on stuff; social, email, etc... Allow them to be built on the DEE / DPs

    // ALTERNATIVE 2 is extend both
    //   -extended-nabu; provides distributed filesystem and exec env
    //   -extended-peergos; provides a normal app node, pki node; the extended-nabu as a submodule, and a mirror/replication node (which only provides an ipfs p2p and relay capability for FS and EE (exec env)
    //        extendedpeergos becomes the NODE software;
    //        extended-nabu (running provdes p2p; resource addressing
    //   -Remove a bunch on stuff; social, email, etc... Allow them to be built on the DEE / DPs
    //   -Remove tiered login and restricted access
    //   - Remake the UI to provide a user and an admin UI; admin can edit priorities / monitor the network / add/remove users
    //          Remove the embedded UI, make integration as a flutter app interacting with an RPC API

    // ALTERNATIVE 3 is merge both and extend to change the orientation to a Resources API and network
    // Since DIFFERENCE I NEED TO IMPLEMENT:
//    light-nodes; i.e. that run on phone devices
//    full-nodes; that dont simply provide a gateway for login and mgt of ipfs data , social email etc..
//    but expose the full Resources API; thus integration provides pki + mirroring capy, but also is a proxy to the ipfs (DFS) & distrib exec env (DEE)
//                * then why dont I just merge integration all together??
    // Also need to make use of the ipfs p2p net for the Exec env.
    // This would allow proper integration between resources API and ipfs node not simply as a submodule (that is constrained by ipfs itself) but a functional component within
    //       This might also be important for when I need to provide intrinsic priorities data transport flows
    // The so-called gateway server is removed for the function integration provides (as an entry point to a node)
    //       It is replaced with a Resources API (which is an entry point to the entire Resource network, just through that gateway as a stepping-in point
    //       But then I add the http gateway back (in a different form), just as the admin app that allows management of the Resource network


    // TODO,
    // APIService needs to accept a custom call type (NDR/DP)
    //      In Client, I make this call
    // In APIService, integration should be able to retrieve the referenced bytecode, take params and execute with params
    //                integration should be able to deploy new DPs, accepting bytecode and storing in local node
    //                i.e. I write a DP in java, that updates a database entry (from input params), then compile integration and deploy to the net
    //                     later I call the update DB DP with table, id, new val

    // I have a concept of a full node & light node/
    // Some apps may use a DP on a full node; or its own light node; i.e. that runs on a mobile
    //  e.g. messaging app options;
    //      Central DP on full node that only routes messages to the known (replicated) DP or node closest to user
    //      Central DP on full node that allows proxy routing; i.e. a message from a user (web browser) goes to the full node and is routed to the intended recipient DP, then proxied to user destination
    //      Mobile app client with light node (that only provides idDHT for identity lookups), can request user locs from the network without any intermediary
    //            ** In the messaging case; messages are stored on a node somewhere, that are delivered by the user/client needing to check for new messages

    // DPs may be stateless or stateful
    //   Nodes may be configured to allow replication of stateless only, or specified stateful DPs
    //   If replicating stateful DPs they must be authorised from the developer.
    //   Stateless: something that uses the global network state entirely
    //   Statefull: something that requires DP state to function (private lists etc...)

    // TODO, I NEED TO FIRST HAVE A ROUTINE THAT ADDS SOME CANNED DATA TO A LOCALNODE, THEN I RUN MULTIPLE SERVERS, AND BOOTSTRAP OFF A DIFFERENT LOCAL SERVER

    public Server(Integer instance_id) throws Exception {
        // For local testing; Allow each server to start as a seperate ipfs-node
        Path ipfsPath;
        if (instance_id != null) {
            ipfsPath = Path.of(System.getenv("HOME"), ".ipfs" + instance_id);
        } else {
            ipfsPath = Path.of(System.getenv("HOME"), ".ipfs");
        }
        Logging.init(ipfsPath);

        /* NOTE: config is here: ~/.ipfs/config */
        // Configs need to say what DPs are allowed on this node; specific pubkeys only (to function like a legacy webserver); specific types or all

        Config config = readConfig(ipfsPath, instance_id);
        LOG.fine("Starting Nabu version: " + APIService.CURRENT_VERSION);

        Path blocksPath = ipfsPath.resolve("blocks");
        File blocksDirectory = blocksPath.toFile();
        if (!blocksDirectory.exists()) {
            if (!blocksDirectory.mkdir()) {
                throw new IllegalStateException("Unable to make blocks directory");
            }
        } else if (blocksDirectory.isFile()) {
            throw new IllegalStateException("Unable to create blocks directory");
        }
        // NOTE: this builds either a FileBlockstore, FilteredBlockstore (bloom filter), or codec type limited
        //        In prod. But for dev tests I just use the RAM blockstore type
        Blockstore blockStore = buildBlockStore(config, blocksPath);

        List<MultiAddress> swarmAddresses = config.addresses.getSwarmAddresses();
        int hostPort = swarmAddresses.get(0).getPort();
        HostBuilder builder = new HostBuilder().setIdentity(config.identity.privKeyProtobuf).listenLocalhost(hostPort);
        if (! builder.getPeerId().equals(config.identity.peerId)) {
            throw new IllegalStateException("PeerId invalid");
        }
        Multihash ourPeerId = Multihash.deserialize(builder.getPeerId().getBytes());

        Path datastorePath = ipfsPath.resolve("datastore").resolve("h2.datastore");
        DatabaseRecordStore records = new DatabaseRecordStore(datastorePath.toString());
        ProviderStore providers = new RamProviderStore();
        Kademlia dht = new Kademlia(new KademliaEngine(ourPeerId, providers, records), false);

        /// Additional datastores and DHTs for Identity and DPs

//        /* DP datastore and DHT ?? */
//        Path datastorePathDp = ipfsPath.resolve("dp-datastore").resolve("h2.datastore");
//        DatabaseRecordStore recordsDp = new DatabaseRecordStore(datastorePath.toString());
//        ProviderStore providersDp = new RamProviderStore();
//        Kademlia dpDht = new Kademlia(new KademliaEngine(ourPeerId, providersDp, recordsDp), false);

        /* ID, user/client DHT */
        /// OR PERHAPS IT ISNT A DHT, IT IS A REPLICATED PKI DATASTORE LIKE ON PEERGOS
        ///  OR BUILD IT LIKE this PkiCache on peergos: https://github.com/Peergos/Peergos/blob/master/src/peergos/server/JdbcPkiCache.java
//        Path datastorePathId = ipfsPath.resolve("id-datastore").resolve("h2.datastore");
//        DatabaseRecordStore recordsId = new DatabaseRecordStore(datastorePath.toString());
//        ProviderStore providersId = new RamProviderStore();
//        Kademlia idDht = new Kademlia(new KademliaEngine(ourPeerId, providersId, recordsId), false);

        CircuitStopProtocol.Binding stop = new CircuitStopProtocol.Binding();
        CircuitHopProtocol.RelayManager relayManager = CircuitHopProtocol.RelayManager.limitTo(builder.getPrivateKey(), ourPeerId, 5);
        BlockRequestAuthoriser authoriser = (c, b, p, a) -> CompletableFuture.completedFuture(true);
        builder = builder.addProtocols(List.of(
                new Ping(),
                new AutonatProtocol.Binding(),
                new CircuitHopProtocol.Binding(relayManager, stop),
                // REPLACE Bitswap protocol with ResSwap which is bitswap extended with distributed computation capy
                // FOR NOW USING THE SAME BLOCK STORE TO STORE DPs as if they are blocks
                //new Bitswap(new BitswapEngine(blocks, authoriser)),
                new ResSwap(new ResSwapEngine(blockStore, authoriser)),
                dht));

        Host node = builder.build();
        node.start().join();
        info("Node started and listening on " + node.listenAddresses());
        info("Starting bootstrap process");


        int connections = dht.bootstrapRoutingTable(node, config.bootstrap.getBootstrapAddresses(), addr -> !addr.contains("/wss/"));
//        if (connections == 0)
//            throw new IllegalStateException("No connected peers!");
        dht.bootstrap(node);

        MultiAddress apiAddress = config.addresses.apiAddress;
        InetSocketAddress localAPIAddress = new InetSocketAddress(apiAddress.getHost(), apiAddress.getPort());

        int maxConnectionQueue = 500;
        int handlerThreads = 50;
        info("Starting RPC API server at: localhost:" + localAPIAddress.getPort());
        HttpServer apiServer = HttpServer.create(localAPIAddress, maxConnectionQueue);

        APIService service = new APIService(
                blockStore,
                //new BitswapBlockService(node, builder.getBitswap().get()), // REPLACED WITH ResourceService
                dht,
                new ResourceServiceImpl(node, builder.getResSwap().get()),
                new RamBlockstore() // TODO, IN PROD THIS SHOULD BE A MORE PERMANENT STORE; FILESTORE, FILTER STORE
                );
        apiServer.createContext(APIService.API_URL, new APIHandler(service, node));
        apiServer.setExecutor(Executors.newFixedThreadPool(handlerThreads));
        apiServer.start();

        int connectionsB = dht.bootstrapRoutingTable(node, config.bootstrap.getBootstrapAddresses(), addr -> !addr.contains("/wss/"));
        LOG.info("BoostrapConnectionsB: "+connectionsB);

//        Bitswap bitswap = builder.getBitswap().get();
//        bitswap.

        Thread shutdownHook = new Thread(() -> {
            info("Stopping server...");
            try {
                node.stop().get();
                apiServer.stop(3); //wait max 3 seconds
                records.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }
    private void info(String message) {
        LOG.info(message);
        System.out.println(message);
    }

    private Config readConfig(Path configPath, Integer instance_id) throws IOException {
        Path configFilePath = configPath.resolve("config");
        File configFile = configFilePath.toFile();
        if (!configFile.exists()) {
            info("Unable to find config file. Creating default config: "+configPath);
            Config config = new Config(instance_id);
            Files.write(configFilePath, config.toString().getBytes(), StandardOpenOption.CREATE);
            return config;
        }
        return Config.build(Files.readString(configFilePath), instance_id);
    }

    public static void main(String[] args) {
        try {
            // I pass an instance ID just for testing, spinning up multiple instances with different configs on DEV ENV
            if (args.length>0) {
                int instance_id = Integer.parseInt(args[0]);
                new Server(instance_id);
            } else {
                // Start as normal
                new Server(null);
            }

        } catch (ParseException pe) {
            LOG.severe("Invalid argument, must be an integer representing instance_id");
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "SHUTDOWN", e);
        }
    }
}