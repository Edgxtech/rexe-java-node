package tech.edgx.rexe.service;

import io.libp2p.core.PeerId;
import org.peergos.HashedBlock;
import org.peergos.Want;
import tech.edgx.rexe.model.dp.DpResult;
import tech.edgx.rexe.model.dp.DpWant;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public interface ResourceService {

    List<HashedBlock> get(List<Want> hashes, Set<PeerId> peers, boolean addToBlockstore);

    default HashedBlock get(Want c, Set<PeerId> peers, boolean addToBlockstore) {
        return get(Collections.singletonList(c), peers, addToBlockstore).get(0);
    }

    List<DpResult> compute(List<DpWant> hashes, Set<PeerId> peers, boolean addToBlockstore);

    default DpResult compute(DpWant c, Set<PeerId> peers, boolean addToBlockstore) {
        return compute(Collections.singletonList(c), peers, addToBlockstore).get(0);
    }
}
