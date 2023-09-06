package tech.edgx.dee.service;

import io.libp2p.core.Host;
import io.libp2p.core.PeerId;
import org.peergos.HashedBlock;
import org.peergos.Want;
import tech.edgx.dee.model.dp.DpResult;
import tech.edgx.dee.model.dp.DpWant;
import tech.edgx.dee.protocol.resswap.ResSwap;

import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ResourceServiceImpl implements ResourceService {

    private static final Logger LOG = Logger.getLogger(ResourceServiceImpl.class.getName());

    private final Host us;
    private final ResSwap resSwap;

    public ResourceServiceImpl(Host us, ResSwap resSwap) {
        this.us = us;
        this.resSwap = resSwap;
    }

    @Override
    public List<HashedBlock> get(List<Want> hashes, Set<PeerId> peers, boolean addToBlockstore) {
        LOG.info("Requesting wants, #: "+hashes.size());
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
