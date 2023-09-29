package tech.edgx.rexe.dp_helloworld;

import com.sun.net.httpserver.HttpServer;
import io.ipfs.cid.Cid;
import io.ipfs.multiaddr.MultiAddress;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.peergos.*;
import org.peergos.blockstore.ProvidingBlockstore;
import org.peergos.blockstore.RamBlockstore;
import org.peergos.net.APIHandler;
import org.peergos.protocol.dht.Kademlia;
import tech.edgx.rexe.client.RexeClient;
import tech.edgx.rexe.util.Helpers;
import tech.edgx.rexe.util.HexUtil;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.Executors;

public class HelloWorldDpTest {

    static RexeClient rexeClient;

    @BeforeClass
    public static void setUp() {
        HttpServer apiServer = null;
        try {
            MultiAddress apiAddress = new MultiAddress("/ip4/127.0.0.1/tcp/8123");
            InetSocketAddress localAPIAddress = new InetSocketAddress(apiAddress.getHost(), apiAddress.getPort());

            apiServer = HttpServer.create(localAPIAddress, 500);
            EmbeddedIpfs ipfs = new EmbeddedIpfs(null, new ProvidingBlockstore(new RamBlockstore()), null, new Kademlia(null, false), null, Optional.empty(), Collections.emptyList());
            apiServer.createContext(APIHandler.API_URL, new APIHandler(ipfs));

            apiServer.setExecutor(Executors.newFixedThreadPool(50));
            apiServer.start();

            rexeClient = new RexeClient(apiAddress.getHost(), apiAddress.getPort(), "/api/v0/", false);
            String version = rexeClient.version();
            Assert.assertTrue("version", version != null);
        }
        catch (Exception e) {e.printStackTrace();}
    }

    @Test
    public void computeDpTestAddFunction() {
        try {
            String testDpName = "dp/TestDp.jar";
            File jarFile = new File(testDpName);
            Helpers.printJarInfo(jarFile);
            byte[] bytecode = Files.readAllBytes(jarFile.toPath());
            Cid addedHash = rexeClient.put(bytecode, Optional.of("raw"));

            boolean has = rexeClient.hasBlock(addedHash, Optional.empty());
            Assert.assertTrue("has block as expected", has);

            Object result = rexeClient.compute(addedHash, Optional.empty(), "tech.edgx.dp.testdp.DP:add", Optional.of(new String[]{"1.12312","3.232432"}),Optional.empty());
            print("DP compute result: "+result);

        } catch (IOException ioe) {
            ioe.printStackTrace();
            Assert.assertTrue("IOException", false);
        }
    }

    @Test
    public void computeDpTestHelloWorldNoParams() {
        try {
            String testDpName = "dp/TestDp.jar";
            File jarFile = new File(testDpName);
            Helpers.printJarInfo(jarFile);
            byte[] bytecode = Files.readAllBytes(jarFile.toPath());
            Cid addedHash = rexeClient.put(bytecode, Optional.of("raw"));

            boolean has = rexeClient.hasBlock(addedHash, Optional.empty());
            Assert.assertTrue("has block as expected", has);

            Object result = rexeClient.compute(addedHash, Optional.empty(), "tech.edgx.dp.testdp.DP:getTestVal", Optional.empty(), Optional.empty());
            print("DP compute result: "+result);

        } catch (IOException ioe) {
            ioe.printStackTrace();
            Assert.assertTrue("IOException", false);
        }
    }

    @Test
    public void putAndRemoveDpTest() {
        try {
            String testDpName = "dp/TestDp.jar";
            File jarFile = new File(testDpName);
            Helpers.printJarInfo(jarFile);
            byte[] bytecode = Files.readAllBytes(jarFile.toPath());
            Cid addedHash = rexeClient.put(bytecode, Optional.of("raw"));

            int size  = rexeClient.stat(addedHash);
            print("Size: "+size+", Orig size: "+jarFile.length());
            Assert.assertTrue("size as expected", size == jarFile.length());

            boolean has = rexeClient.hasBlock(addedHash, Optional.empty());
            Assert.assertTrue("has block as expected", has);

            boolean bloomAdd = rexeClient.bloomAdd(addedHash);
            Assert.assertTrue("added to bloom filter", !bloomAdd); //RamBlockstore does not filter

            byte[] data = rexeClient.getBlock(addedHash, Optional.empty());
            print("Recovered bytecode: "+HexUtil.encodeHexString(data));
            Assert.assertTrue("block is as expected", HexUtil.encodeHexString(bytecode).equals(HexUtil.encodeHexString(data)));

            List<Cid> localRefs = rexeClient.listBlockstore();
            Assert.assertTrue("local ref size", localRefs.size() == 1);

            rexeClient.removeBlock(addedHash);
            List<Cid> localRefsAfter = rexeClient.listBlockstore();
            Assert.assertTrue("local ref size after rm", localRefsAfter.size() == 0);

            boolean have = rexeClient.hasBlock(addedHash, Optional.empty());
            Assert.assertTrue("does not have block as expected", !have);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            Assert.assertTrue("IOException", false);
        }
    }

    public void print(String msg) {
        System.out.println(msg);
    }
}
