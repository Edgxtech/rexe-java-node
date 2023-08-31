package tech.edgx.service;

import io.libp2p.core.Host;
import io.libp2p.core.PeerId;
import org.peergos.BlockService;
import org.peergos.HashedBlock;
import org.peergos.Want;
import org.peergos.protocol.bitswap.Bitswap;
import tech.edgx.model.dp.DpResult;
import tech.edgx.model.dp.DpWant;
import tech.edgx.protocol.dpswap.Dpswap;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class DpswapDpResultService implements DpResultService {

    private final Host us;
    private final Dpswap dpswap;

    public DpswapDpResultService(Host us, Dpswap dpswap) {
        this.us = us;
        this.dpswap = dpswap;
    }

    @Override
    public List<DpResult> get(List<DpWant> hashes, Set<PeerId> peers, boolean addToBlockstore) {
        return dpswap.get(hashes, us, peers, addToBlockstore)
                .stream()
                .map(f -> f.join())
                .collect(Collectors.toList());
    }
}
