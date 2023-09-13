package tech.edgx.dee.service;

import io.libp2p.core.PeerId;
import org.peergos.HashedBlock;
import tech.edgx.dee.model.dp.DpResult;
import tech.edgx.dee.model.dp.DpWant;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public interface ComputeService {

//    List<HashedBlock> get(List<DpWant> hashes, Set<PeerId> peers, boolean addToBlockstore);
//
//    default HashedBlock get(DpWant c, Set<PeerId> peers, boolean addToBlockstore) {
//        return get(Collections.singletonList(c), peers, addToBlockstore).get(0);
//    }

    List<DpResult> compute(List<DpWant> hashes, Set<PeerId> peers, boolean addToBlockstore);

    default DpResult compute(DpWant c, Set<PeerId> peers, boolean addToBlockstore) {
        return compute(Collections.singletonList(c), peers, addToBlockstore).get(0);
    }
}
