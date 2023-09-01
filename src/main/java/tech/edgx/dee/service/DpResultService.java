package tech.edgx.dee.service;

import io.libp2p.core.PeerId;
import tech.edgx.dee.model.dp.DpResult;
import tech.edgx.dee.model.dp.DpWant;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public interface DpResultService {

    List<DpResult> get(List<DpWant> hashes, Set<PeerId> peers, boolean addToBlockstore);

    default DpResult get(DpWant c, Set<PeerId> peers, boolean addToBlockstore) {
        return get(Collections.singletonList(c), peers, addToBlockstore).get(0);
    }
}
