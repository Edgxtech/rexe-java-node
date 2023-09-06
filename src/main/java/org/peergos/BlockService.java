package org.peergos;

import io.libp2p.core.*;
import tech.edgx.dee.util.SwapType;

import java.util.*;

public interface BlockService {

    List<HashedBlock> get(List<Want> hashes, Set<PeerId> peers, boolean addToBlockstore); //, SwapType swapType

    default HashedBlock get(Want c, Set<PeerId> peers, boolean addToBlockstore) { //, SwapType swapType
        return get(Collections.singletonList(c), peers, addToBlockstore).get(0);
    }
}
