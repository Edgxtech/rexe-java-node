package tech.edgx.rexe.ebitswap;

import io.ipfs.cid.Cid;
import io.libp2p.core.Host;
import io.libp2p.core.multiformats.Multiaddr;
import org.junit.Assert;
import org.junit.Test;
import org.peergos.HashedBlock;
import org.peergos.HostBuilder;
import org.peergos.Want;
import org.peergos.blockstore.RamBlockstore;
import org.peergos.protocol.dht.RamProviderStore;
import org.peergos.protocol.dht.RamRecordStore;
import tech.edgx.rexe.model.dp.DpResult;
import tech.edgx.rexe.model.dp.DpWant;
import tech.edgx.rexe.protocol.ebitswap.eBitSwap;
import tech.edgx.rexe.util.Helpers;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class eBitSwapTest {

    @Test
    public void computeDpHelloWorldFunction() {
        HostBuilder builder1 = HostBuilder.create(10000 + new Random().nextInt(50000),
                new RamProviderStore(), new RamRecordStore(), new RamBlockstore(), (c, b, p, a) -> CompletableFuture.completedFuture(true));
        Host node1 = builder1.build();

        RamBlockstore blockstore2 = new RamBlockstore();
        HostBuilder builder2 = HostBuilder.create(10000 + new Random().nextInt(50000),
                new RamProviderStore(), new RamRecordStore(), blockstore2, (c, b, p, a) -> CompletableFuture.completedFuture(true));
        Host node2 = builder2.build();
        node1.start().join();
        node2.start().join();
        try {
            Multiaddr address2 = node2.listenAddresses().get(0);
            String testDpName = "dp/TestDp.jar";
            File jarFile = new File(testDpName);
            Helpers.printJarInfo(jarFile);
            byte[] bytecode = Files.readAllBytes(jarFile.toPath());
            Cid hash = blockstore2.put(bytecode, Cid.Codec.Raw).join();

            node1.getAddressBook().addAddrs(address2.getPeerId(), 0, address2).join();

            eBitSwap resSwap1 = builder1.getResSwap().get();
            DpWant dpWant = new DpWant(hash, Optional.empty(), "tech.edgx.dp.testdp.DP:getTestVal", Optional.empty(), Optional.empty());
            System.out.println("Sending compute request: "+dpWant.cid+", functionname: "+dpWant.functionName + ", params: "+dpWant.params +", auth: "+dpWant.auth);
            List<DpResult> receivedResults = resSwap1.compute(List.of(dpWant), node1, Set.of(address2.getPeerId()), false) //Set.of(address2.getPeerId())
                    .stream()
                    .map(f -> f.join())
                    .collect(Collectors.toList());
            Assert.assertEquals("Correct result", receivedResults.get(0).result.toString(),"Hello World");
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
        finally {
            node1.stop();
            node2.stop();
        }
    }

    @Test
    public void computeDpAddFunction() {
        HostBuilder builder1 = HostBuilder.create(10000 + new Random().nextInt(50000),
                new RamProviderStore(), new RamRecordStore(), new RamBlockstore(), (c, b, p, a) -> CompletableFuture.completedFuture(true));
        Host node1 = builder1.build();
        RamBlockstore blockstore2 = new RamBlockstore();
        HostBuilder builder2 = HostBuilder.create(10000 + new Random().nextInt(50000),
                new RamProviderStore(), new RamRecordStore(), blockstore2, (c, b, p, a) -> CompletableFuture.completedFuture(true));
        Host node2 = builder2.build();
        node1.start().join();
        node2.start().join();
        try {
            Multiaddr address2 = node2.listenAddresses().get(0);
            String testDpName = "dp/TestDp.jar";
            File jarFile = new File(testDpName);
            Helpers.printJarInfo(jarFile);
            byte[] bytecode = Files.readAllBytes(jarFile.toPath());
            Cid hash = blockstore2.put(bytecode, Cid.Codec.Raw).join();
            node1.getAddressBook().addAddrs(address2.getPeerId(), 0, address2).join();
            eBitSwap resSwap1 = builder1.getResSwap().get();
            double val1 = 322;
            double val2 = 43.432423;
            DpWant dpWant = new DpWant(hash, Optional.empty(), "tech.edgx.dp.testdp.DP:add", Optional.of(new String[]{String.valueOf(val1),String.valueOf(val2)}), Optional.empty());
            System.out.println("Sending compute request: "+dpWant.cid+", functionname: "+dpWant.functionName + ", params: "+dpWant.params +", auth: "+dpWant.auth);
            List<DpResult> receivedResults = resSwap1.compute(List.of(dpWant), node1, Set.of(address2.getPeerId()), false)
                    .stream()
                    .map(f -> f.join())
                    .collect(Collectors.toList());
            System.out.println("Received result: "+ receivedResults.get(0).result+", vs expected: "+String.valueOf(val1 + val2));
            Assert.assertEquals("Correct result", receivedResults.get(0).result.toString(),String.valueOf(val1 + val2));
        } catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            node1.stop();
            node2.stop();
        }
    }

    @Test
    public void getBlock() {
        // NODE 1
        HostBuilder builder1 = HostBuilder.create(10000 + new Random().nextInt(50000),
                new RamProviderStore(), new RamRecordStore(), new RamBlockstore(), (c, b, p, a) -> CompletableFuture.completedFuture(true));
        Host node1 = builder1.build();

        // NODE 2
        RamBlockstore blockstore2 = new RamBlockstore();
        HostBuilder builder2 = HostBuilder.create(10000 + new Random().nextInt(50000),
                new RamProviderStore(), new RamRecordStore(), blockstore2, (c, b, p, a) -> CompletableFuture.completedFuture(true));
        Host node2 = builder2.build();
        node1.start().join();
        node2.start().join();
        try {
            // Put into 2
            Multiaddr address2 = node2.listenAddresses().get(0);
            byte[] blockData = "G'day from Java ebitSwap!".getBytes(StandardCharsets.UTF_8);
            Cid hash = blockstore2.put(blockData, Cid.Codec.Raw).join();

            // Make 1 aware of 2
            node1.getAddressBook().addAddrs(address2.getPeerId(), 0, address2).join();

            // Request from 1 - should negotiate the swap with 2
            eBitSwap eBitSwap = builder1.getResSwap().get();
            List<HashedBlock> receivedBlock = eBitSwap.get(List.of(new Want(hash)), node1, Set.of(address2.getPeerId()), false) //Set.of(address2.getPeerId())
                    .stream()
                    .map(f -> f.join())
                    .collect(Collectors.toList());
            System.out.println("Received block: "+ receivedBlock.get(0).hash);
            Assert.assertTrue("Correct result", Arrays.equals(receivedBlock.get(0).block, blockData));
        } finally {
            node1.stop();
            node2.stop();
        }
    }
}
