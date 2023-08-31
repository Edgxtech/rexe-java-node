package org.peergos;
import com.google.gson.Gson;
import io.ipfs.cid.Cid;
import io.libp2p.core.*;
import org.peergos.blockstore.Blockstore;
import org.peergos.protocol.dht.Kademlia;
import org.peergos.util.Logging;
import org.peergos.util.Version;
import tech.edgx.model.dp.DpResult;
import tech.edgx.model.dp.DpWant;
import tech.edgx.service.DpResultService;
import tech.edgx.service.RuntimeService;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.*;

public class APIService {

    private static final Logger LOG = Logging.LOG();
    public static final Version CURRENT_VERSION = Version.parse("0.0.1");
    public static final String API_URL = "/api/v0/";

    private final Blockstore store;
    private final BlockService remoteBlocks;

    private final Kademlia dht;

    private final Blockstore dpStore;
    private final DpResultService dpResultService;
    private final RuntimeService runtimeService;

    public APIService(Blockstore store, BlockService remoteBlocks, Kademlia dht, DpResultService dpResultService, Blockstore dpStore) {
        this.store = store;
        this.remoteBlocks = remoteBlocks;
        this.dht = dht;

        //this.dpDht = dpDht; // Reusing the DHT for now
        this.dpStore = dpStore;
        this.runtimeService = new RuntimeService();
        this.dpResultService = dpResultService;
    }

    public List<HashedBlock> getBlocks(List<Want> wants, Set<PeerId> peers, boolean addToLocal) {
        List<HashedBlock> blocksFound = new ArrayList<>();

        List<Want> local = new ArrayList<>();
        List<Want> remote = new ArrayList<>();

        for (Want w : wants) {
            if (store.has(w.cid).join())
                local.add(w);
            else
                remote.add(w);
        }
        local.stream()
                .map(w -> new HashedBlock(w.cid, store.get(w.cid).join().get()))
                .forEach(blocksFound::add);
        if (remote.isEmpty())
            return blocksFound;
        return java.util.stream.Stream.concat(
                        blocksFound.stream(),
                        remoteBlocks.get(remote, peers, addToLocal).stream())
                .collect(Collectors.toList());
    }
    
    public Cid putBlock(byte[] block, Cid.Codec codec) {
        return store.put(block, codec).join();
    }

    public Boolean rmBlock(Cid cid) {
        return store.rm(cid).join();
    }

    public Boolean hasBlock(Cid cid) {
        return store.has(cid).join();
    }
    public List<Cid> getRefs() {
        return store.refs().join();
    }

    public Boolean bloomAdd(Cid cid) {
        return store.bloomAdd(cid).join();
    }

    public List<PeerAddresses> findProviders(Cid cid, Host node, int numProviders) {
        List<PeerAddresses> providers = dht.findProviders(cid, node, numProviders).join();
        return providers;
    }

    //// CUSTOM by TDE - For DRF
    /// Might need to create a unique TYPE of CID, so they don't get confused with normal DATA CIDs
    // Or a way to know if a CID is a DP or not
    public Cid putDp(byte[] dp, Cid.Codec codec) {
        // PUT into dp specific Store
        return dpStore.put(dp, codec).join();
    }

    public Boolean rmDp(Cid cid) {
        return dpStore.rm(cid).join();
    }

    public Boolean hasDp(Cid cid) {
        return dpStore.has(cid).join();
    }

    public Boolean bloomAddDp(Cid cid) {
        return dpStore.bloomAdd(cid).join();
    }

    //public DpCallResult call(Cid cid, String function, String[] params) {
        // Once wants are sent to network, it uses protocol buffers onMsg to listen for msgs, then send to bitswapengine
        // to process the message, which may have any of the prev requested wants/blocks:: MessageOuterClass.Message.Block block : msg.getPayloadList()
        // Or it actually might also have a wants list sent from another node
        //    Bitswap is a mini inner protocol just to exchange blocks in request to wants - perhaps can reuse but it will be DP Requests are the "wants", DP Results are the "blocks"
        //       perhaps can re-use; DP requests (wants) contain (CID, functionName, Params, est_computation) ; other nodes that see it may decide not to process because computing resource too high
        //                           DP results (~= "blocks" contain serlialised object data according to spec of the DP
        //        @Override
        //        public void onMessage(@NotNull Stream stream, MessageOuterClass.Message msg) {
        //            engine.receiveMessage(msg, stream);
        //        }
        //    Then it checks local list of wants it is waiting for, and those it wants to persist, and completes the Future<Block> so the data is available in the wantsList
        //        CompletableFuture<HashedBlock> waiter = localWants.get(w);
        //        if (waiter != null) {
        //            if (persistBlocks.containsKey(w)) {
        //                store.put(data, codec);
        //                persistBlocks.remove(w);
        //            }
        //            waiter.complete(new HashedBlock(c, data));
        //            localWants.remove(w);
        //     Every msg is a chance to fulfill a want
        //     rxMsg also, in return sendsMessagesBack in two different spots incl at the end:
        //         buildAndSendMessages(wants, presences, blocks, source::writeAndFlush);
        //         * otherwise buildAndSend is called only from the Bitswap [controller]::sendWants() which is sent
        //         in response to a getWants/Blocks request

        // Optionally constrains to List<Peer> peers, otherwise uses all connected peers in the p2p net
        // broadcast to all connected peers if none are supplied
        //Set<PeerId> connected = peers.isEmpty() ? engine.getConnected() : peers;


    // Instruct the node hosting it to execute DP and return result
    // Still uses bitswap-like protocol to send and process Wants
    public List<DpResult> computeDp(List<DpWant> wants, Set<PeerId> peers, boolean addToLocal) {
        List<DpResult> resultsComputed = new ArrayList<>();
        List<DpWant> local = new ArrayList<>();
        List<DpWant> remote = new ArrayList<>();
        for (DpWant w : wants) {
            if (dpStore.has(w.cid).join())
                // compute locally
                local.add(w);
            else
                // compute remotely
                remote.add(w);
        }
        LOG.fine("Computing DPs locally: "+new Gson().toJson(local));
        LOG.fine("Computing DPs remote: "+new Gson().toJson(remote));
        local.stream()
                .map(w -> {
                    try {
                        LOG.fine("Executing: "+new Gson().toJson(w));
                        return runtimeService.runDp(w.cid, store.get(w.cid).join().get(), w.functionName, w.params);
                    } catch(Exception e) {
                        LOG.fine("Failed to execute DP: "+w.cid.toString());
                        return null;
                    }
                })
                .forEach(resultsComputed::add);
        if (remote.isEmpty())
            return resultsComputed;

        LOG.warning("Temp only executing locally");
        return resultsComputed;
        // return merged list of locally & remote found blocks
//        return java.util.stream.Stream.concat(
//                        resultsComputed.stream(),
//                        // This begins the sendWants, listen for received msgs incl blocks and fulfil the wants when rx'd
//                        dpResultService.get(remote, peers, addToLocal).stream()) // equiv to remoteBlocks[BlocksService].get()
//                .collect(Collectors.toList());
    }
}

// CONSIDERED SIMPLY FINDING A PROVIDER, THEN EXECUTING ON THE PROVIDER BY RPC
// I just need to find a host with the DP I need (using the dht), then use an RPC service to make the call
//     instead of for e.g. data retrieval it uses BitswapBlockService to request remote blocks (if not held locally)
// findProviders then RPC the service; I dont think this is the right approach here, findProviders in ipfs is only used as a default API method, probably for admin/mgt use only
//PeerAddresses peerAddresses = findProviders(cid);
// TODO, potentially replace findProviders with similar concept of getBlocks (wants, Optional<peers>)
//       which looks in local node for the CID, otherwise transmits to network as wants sed Bitswap::sendWants()
//       Bitswap Engine (within Bitswap [controller]) maintains a list of wants, which are sent to the network
//       On get(List<Want> hash,) it uses Bitswap engine to get indiv wants UNTIL no more wants
//            while (engine.hasWants()) {
//                try {Thread.sleep(5_000);} catch (InterruptedException e) {}
//                sendWants(us, peers);
//            }