package org.peergos;
import io.ipfs.cid.Cid;
import io.libp2p.core.*;
import org.peergos.blockstore.Blockstore;
import org.peergos.protocol.dht.Kademlia;
import org.peergos.util.Version;

import java.util.*;
import java.util.stream.*;

public class APIService {

    public static final Version CURRENT_VERSION = Version.parse("0.0.1");
    public static final String API_URL = "/api/v0/";

    private final Blockstore store;
    private final BlockService remoteBlocks;

    private final Kademlia dht;

    //private final Kademlia dpDht;


    public APIService(Blockstore store, BlockService remoteBlocks, Kademlia dht) {
        this.store = store;
        this.remoteBlocks = remoteBlocks;
        this.dht = dht;
        //this.dpDht = dpDht;
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

    public Cid putDp(byte[] dp, Cid.Codec codec) {
        return store.put(dp, codec).join();
    }
    public Boolean rmDp(Cid cid) {
        return store.rm(cid).join();
    }

    public Boolean hasDp(Cid cid) {
        return store.has(cid).join();
    }

    public Boolean bloomAddDp(Cid cid) {
        return store.bloomAdd(cid).join();
    }

//    public List<PeerAddresses> findProvidersOfDp(Cid cid, Host node, int numProviders) {
//        List<PeerAddresses> providers = dpDht.findProviders(cid, node, numProviders).join();
//        return providers;
//    }

}
