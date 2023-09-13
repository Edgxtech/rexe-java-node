package tech.edgx.drf.service;

import io.libp2p.core.Host;
import io.libp2p.core.PeerId;
import org.peergos.HashedBlock;
import org.peergos.Want;
import org.peergos.protocol.dht.Kademlia;
import tech.edgx.drf.model.dp.DpResult;
import tech.edgx.drf.model.dp.DpWant;
import tech.edgx.drf.protocol.resswap.ResSwap;

import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ResourceServiceImpl implements ResourceService {

    private static final Logger LOG = Logger.getLogger(ResourceServiceImpl.class.getName());

    private final Host us;
    private final ResSwap resSwap;
    //private final Bitswap bitswap;
    private final Kademlia dht;

    public ResourceServiceImpl(Host us, ResSwap resSwap, Kademlia dht) {
        this.us = us;
        this.resSwap = resSwap;
        //this.bitswap = bitswap;
        this.dht = dht;
    }

    @Override
    public List<HashedBlock> get(List<Want> hashes, Set<PeerId> peers, boolean addToBlockstore) {
        LOG.info("Requesting wants, #: "+hashes.size() + "Peers #: "+peers.size());
        return resSwap.get(hashes, us, peers, addToBlockstore)
                .stream()
                .map(f -> f.join())
                .collect(Collectors.toList());
    }

    @Override
    public List<DpResult> compute(List<DpWant> hashes, Set<PeerId> peers, boolean addToBlockstore) {
        LOG.info("Requesting dpwants, #: "+hashes.size());
        return resSwap.compute(hashes, us, peers, addToBlockstore)
                .stream()
                .map(f -> f.join())
                .collect(Collectors.toList());
    }
}
