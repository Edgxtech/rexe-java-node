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
import tech.edgx.dee.model.dp.DpResult;
import tech.edgx.dee.model.dp.DpWant;
import tech.edgx.dee.protocol.resswap.ResSwap;
import tech.edgx.dee.util.Helpers;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class ResSwapTest {

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
            byte[] blockData = "G'day from Java resSwap!".getBytes(StandardCharsets.UTF_8);
            Cid hash = blockstore2.put(blockData, Cid.Codec.Raw).join();

            // Make 1 aware of 2
            // I thought if they bootstrap off the same node they should discover each other
            node1.getAddressBook().addAddrs(address2.getPeerId(), 0, address2).join();

            // Request from 1 - which should negotiate the swap with 2 after 2 responds integration has integration locally
            ResSwap resSwap1 = builder1.getResSwap().get();
            List<HashedBlock> receivedBlock = resSwap1.get(List.of(new Want(hash)), node1, Set.of(address2.getPeerId()), false) //Set.of(address2.getPeerId())
                    .stream()
                    .map(f -> f.join())
                    .collect(Collectors.toList());
            System.out.println("Received block: "+ receivedBlock.get(0).hash);
            if (! Arrays.equals(receivedBlock.get(0).block, blockData))
                throw new IllegalStateException("Incorrect block returned!");
        } finally {
            node1.stop();
            node2.stop();
        }
    }

    @Test
    public void computeDpHelloWorldFunction() {
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
//            byte[] blockData = "G'day from Java resSwap!".getBytes(StandardCharsets.UTF_8);
            String testDpName = "src/main/resources/TestDp.jar";
            File jarFile = new File(testDpName);
            Helpers.printJarInfo(jarFile);
            byte[] bytecode = Files.readAllBytes(jarFile.toPath());
            Cid hash = blockstore2.put(bytecode, Cid.Codec.Raw).join();

            // Make 1 aware of 2
            // I thought if they bootstrap off the same node they should discover each other
            node1.getAddressBook().addAddrs(address2.getPeerId(), 0, address2).join();

            // Request from 1 - which should negotiate the swap with 2 after 2 responds integration has integration locally
            ResSwap resSwap1 = builder1.getResSwap().get();
            DpWant dpWant = new DpWant(hash, Optional.empty(), "getTestVal", Optional.of(new Object[]{""}));
            System.out.println("Sending compute request: "+dpWant.cid+", functionname: "+dpWant.functionName + ", params: "+dpWant.params +", auth: "+dpWant.auth);
            List<DpResult> receivedResults = resSwap1.compute(List.of(dpWant), node1, Set.of(address2.getPeerId()), false) //Set.of(address2.getPeerId())
                    .stream()
                    .map(f -> f.join())
                    .collect(Collectors.toList());
            System.out.println("Received result: "+ receivedResults.get(0).result);

            if (!receivedResults.get(0).result.equals("MY DP test val")) {
                throw new IllegalStateException("Incorrect result returned!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            node1.stop();
            node2.stop();
        }
    }

    @Test
    public void computeDpAddFunction() {
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
//            byte[] blockData = "G'day from Java resSwap!".getBytes(StandardCharsets.UTF_8);
            String testDpName = "src/main/resources/TestDp.jar";
            File jarFile = new File(testDpName);
            Helpers.printJarInfo(jarFile);
            byte[] bytecode = Files.readAllBytes(jarFile.toPath());
            Cid hash = blockstore2.put(bytecode, Cid.Codec.Raw).join();

            // Make 1 aware of 2
            // I thought if they bootstrap off the same node they should discover each other
            node1.getAddressBook().addAddrs(address2.getPeerId(), 0, address2).join();

            // Request from 1 - which should negotiate the swap with 2 after 2 responds integration has integration locally
            ResSwap resSwap1 = builder1.getResSwap().get();
            double val1 = 1032;
            double val2 = 43.432423;
            DpWant dpWant = new DpWant(hash, Optional.empty(), "add", Optional.of(new String[]{String.valueOf(val1),String.valueOf(val2)}));
            System.out.println("Sending compute request: "+dpWant.cid+", functionname: "+dpWant.functionName + ", params: "+dpWant.params +", auth: "+dpWant.auth);
            List<DpResult> receivedResults = resSwap1.compute(List.of(dpWant), node1, Set.of(address2.getPeerId()), false) //Set.of(address2.getPeerId())
                    .stream()
                    .map(f -> f.join())
                    .collect(Collectors.toList());
            System.out.println("Received result: "+ receivedResults.get(0).result);

            if (!receivedResults.get(0).result.equals(String.valueOf(val1 + val2))) {
                throw new IllegalStateException("Incorrect result returned!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            node1.stop();
            node2.stop();
        }
    }
}
