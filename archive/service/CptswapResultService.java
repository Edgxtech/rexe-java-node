package tech.edgx.dee.service;

import io.libp2p.core.Host;
import io.libp2p.core.PeerId;
import org.peergos.HashedBlock;
import tech.edgx.dee.model.dp.DpResult;
import tech.edgx.dee.model.dp.DpWant;
import tech.edgx.dee.protocol.cptswap.Cptswap;

import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class CptswapResultService implements ComputeService {

    private static final Logger LOG = Logger.getLogger(CptswapResultService.class.getName());

    private final Host us;
    private final Cptswap cptswap;

    public CptswapResultService(Host us, Cptswap cptswap) {
        this.us = us;
        this.cptswap = cptswap;
    }

//    @Override
//    public List<HashedBlock> get(List<DpWant> hashes, Set<PeerId> peers, boolean addToBlockstore) {
//        LOG.info("Requsting wants, #: "+hashes.size());
//        return cptswap.get(hashes, us, peers, addToBlockstore)
//                .stream()
//                .map(f -> f.join())
//                .collect(Collectors.toList());
//    }

    @Override
    public List<DpResult> compute(List<DpWant> hashes, Set<PeerId> peers, boolean addToBlockstore) {
        LOG.info("Requesting dpwants, #: "+hashes.size());
        return cptswap.compute(hashes, us, peers, addToBlockstore)
                .stream()
                .map(f -> f.join())
                .collect(Collectors.toList());
    }
}
