package tech.edgx.dee.service;

import io.libp2p.core.PeerId;
import org.peergos.HashedBlock;
import org.peergos.Want;
import tech.edgx.dee.model.dp.DpResult;
import tech.edgx.dee.model.dp.DpWant;

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

    // TODO, brokerStream() ??
    // Send wants for a known large video file hash, if some node has it, sends back a socket or multiaddr enabling
    // client to connect and consume the stream?? Then needs a notion of finishing the stream/closing it
    // Or I dont do this in the reswap protocol, but allow it to be developed as an application over the top in a DP?
    //     i.e. DP holds a dht of stream providers, and can decide which provider to give the requestor
    //          The DP result payload needs to be encodable/decodable according to app developer
    //          So the payload for the known stream brokering DP, returns a multiaddr that provides the streaming service?
}
