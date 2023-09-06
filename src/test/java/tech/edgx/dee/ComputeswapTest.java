package tech.edgx.dee;

import io.ipfs.cid.Cid;
import io.libp2p.core.Host;
import io.libp2p.core.multiformats.Multiaddr;
import org.junit.Test;
import org.peergos.HashedBlock;
import org.peergos.HostBuilder;
import org.peergos.Want;
import org.peergos.blockstore.RamBlockstore;
import org.peergos.protocol.dht.RamProviderStore;
import org.peergos.protocol.dht.RamRecordStore;
import tech.edgx.dee.protocol.cptswap.Cptswap;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class ComputeswapTest {

    @Test
    public void getBlock() {
        // NODE 1
        HostBuilder builder1 = HostBuilder.build(10000 + new Random().nextInt(50000),
                new RamProviderStore(), new RamRecordStore(), new RamBlockstore(), (c, b, p, a) -> CompletableFuture.completedFuture(true));
        Host node1 = builder1.build();

        // NODE 2
        RamBlockstore blockstore2 = new RamBlockstore();
        HostBuilder builder2 = HostBuilder.build(10000 + new Random().nextInt(50000),
                new RamProviderStore(), new RamRecordStore(), blockstore2, (c, b, p, a) -> CompletableFuture.completedFuture(true));
        Host node2 = builder2.build();
        node1.start().join();
        node2.start().join();
        try {
            // Put into 2
            Multiaddr address2 = node2.listenAddresses().get(0);
            byte[] blockData = "G'day from Java cptswap!".getBytes(StandardCharsets.UTF_8);
            Cid hash = blockstore2.put(blockData, Cid.Codec.Raw).join();

            // Make 1 aware of 2
            Cptswap cptswap1 = builder1.getCptswap().get();
            node1.getAddressBook().addAddrs(address2.getPeerId(), 0, address2).join();

            // Request from 1 - which should negotiate the swap with 2 after 2 responds it has it locally
            List<HashedBlock> receivedBlock = cptswap1.get(List.of(new Want(hash)), node1, Set.of(address2.getPeerId()), false)
                    .stream()
                    .map(f -> f.join())
                    .collect(Collectors.toList());
            if (! Arrays.equals(receivedBlock.get(0).block, blockData))
                throw new IllegalStateException("Incorrect block returned!");
        } finally {
            node1.stop();
            node2.stop();
        }
    }
}
