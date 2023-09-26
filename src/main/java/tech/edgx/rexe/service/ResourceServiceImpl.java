package tech.edgx.rexe.service;

import io.libp2p.core.Host;
import io.libp2p.core.PeerId;
import org.peergos.HashedBlock;
import org.peergos.Want;
import org.peergos.protocol.dht.Kademlia;
import tech.edgx.rexe.model.dp.DpResult;
import tech.edgx.rexe.model.dp.DpWant;
import tech.edgx.rexe.protocol.ebitswap.eBitSwap;

import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ResourceServiceImpl implements ResourceService {

    private static final Logger LOG = Logger.getLogger(ResourceServiceImpl.class.getName());

    private final Host us;
    private final eBitSwap eBitSwap;
    private final Kademlia dht;

    public ResourceServiceImpl(Host us, eBitSwap eBitSwap, Kademlia dht) {
        this.us = us;
        this.eBitSwap = eBitSwap;
        this.dht = dht;
    }

    @Override
    public List<HashedBlock> get(List<Want> hashes, Set<PeerId> peers, boolean addToBlockstore) {
        LOG.fine("Requesting wants, #: "+hashes.size() + ", Peers #: "+peers.size());
        return eBitSwap.get(hashes, us, peers, addToBlockstore)
                .stream()
                .map(f -> f.join())
                .collect(Collectors.toList());
    }

    @Override
    public List<DpResult> compute(List<DpWant> hashes, Set<PeerId> peers, boolean addToBlockstore) {
        LOG.fine("Requesting dpwants, #: "+hashes.size() + ", Peers #: "+peers.size());
        return eBitSwap.compute(hashes, us, peers, addToBlockstore)
                .stream()
                .map(f -> f.join())
                .collect(Collectors.toList());
    }
}
