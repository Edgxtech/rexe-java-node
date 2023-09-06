package tech.edgx.dee;

import com.sun.net.httpserver.HttpServer;
import io.ipfs.cid.Cid;
import io.ipfs.multiaddr.MultiAddress;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.peergos.*;
import org.peergos.blockstore.RamBlockstore;
import org.peergos.net.APIHandler;
import tech.edgx.dee.client.DrfClient;
import tech.edgx.dee.service.ResourceServiceImpl;
import tech.edgx.dee.util.Helpers;
import tech.edgx.dee.util.HexUtil;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.Executors;

public class DpTest {

    static DrfClient drfClient;

    @BeforeClass
    public static void setUp() {
        HttpServer apiServer = null;
        try {
            MultiAddress apiAddress = new MultiAddress("/ip4/127.0.0.1/tcp/8123");
            InetSocketAddress localAPIAddress = new InetSocketAddress(apiAddress.getHost(), apiAddress.getPort());

            apiServer = HttpServer.create(localAPIAddress, 500);
            APIService service = new APIService(new RamBlockstore(),null, new ResourceServiceImpl(null, null), new RamBlockstore()); //new BitswapBlockService(null, null),
            apiServer.createContext(APIService.API_URL, new APIHandler(service, null));
            apiServer.setExecutor(Executors.newFixedThreadPool(50));
            apiServer.start();

            drfClient = new DrfClient(apiAddress.getHost(), apiAddress.getPort(), "/api/v0/", false);
            String version = drfClient.version();
            Assert.assertTrue("version", version != null);
        }
        catch (Exception e) {e.printStackTrace();}
    }

    @Test
    public void computeDpTestAddFunction() {
        try {
            //System.out.println("DpClient: "+new Gson().toJson(dpClient));
            String testDpName = "src/main/resources/TestDp.jar";
            File jarFile = new File(testDpName);
            Helpers.printJarInfo(jarFile);
            byte[] bytecode = Files.readAllBytes(jarFile.toPath());
            Cid addedHash = drfClient.put(bytecode, Optional.of("raw"));

            boolean has = drfClient.hasBlock(addedHash, Optional.empty());
            Assert.assertTrue("has block as expected", has);

            String result = drfClient.compute(addedHash, Optional.empty(), "add", Optional.of(new String[]{"1.12312","3.232432"}));
            print("DP compute result: "+result);

        } catch (IOException ioe) {
            ioe.printStackTrace();
            Assert.assertTrue("IOException", false);
        }
    }

    @Test
    public void computeDpTestHellowWorldNoParams() {
        try {
            String testDpName = "src/main/resources/TestDp.jar";
            File jarFile = new File(testDpName);
            Helpers.printJarInfo(jarFile);
            byte[] bytecode = Files.readAllBytes(jarFile.toPath());
            Cid addedHash = drfClient.put(bytecode, Optional.of("raw"));

            boolean has = drfClient.hasBlock(addedHash, Optional.empty());
            Assert.assertTrue("has block as expected", has);

            String result = drfClient.compute(addedHash, Optional.empty(), "getTestVal", Optional.empty());
            print("DP compute result: "+result);

        } catch (IOException ioe) {
            ioe.printStackTrace();
            Assert.assertTrue("IOException", false);
        }
    }

    @Test
    public void putAndRemoveDpTest() {
        try {
            String testDpName = "src/main/resources/TestDp.jar";
            File jarFile = new File(testDpName);
            Helpers.printJarInfo(jarFile);
            byte[] bytecode = Files.readAllBytes(jarFile.toPath());
            Cid addedHash = drfClient.put(bytecode, Optional.of("raw"));

            int size  = drfClient.stat(addedHash);
            print("Size: "+size+", Orig size: "+jarFile.length());
            Assert.assertTrue("size as expected", size == jarFile.length());

            boolean has = drfClient.hasBlock(addedHash, Optional.empty());
            Assert.assertTrue("has block as expected", has);

            boolean bloomAdd = drfClient.bloomAdd(addedHash);
            Assert.assertTrue("added to bloom filter", !bloomAdd); //RamBlockstore does not filter

            byte[] data = drfClient.getBlock(addedHash, Optional.empty());
            print("Recovered bytecode: "+HexUtil.encodeHexString(data));
            Assert.assertTrue("block is as expected", HexUtil.encodeHexString(bytecode).equals(HexUtil.encodeHexString(data)));

            List<Cid> localRefs = drfClient.listBlockstore();
            Assert.assertTrue("local ref size", localRefs.size() == 1);

            drfClient.removeBlock(addedHash);
            List<Cid> localRefsAfter = drfClient.listBlockstore();
            Assert.assertTrue("local ref size after rm", localRefsAfter.size() == 0);

            boolean have = drfClient.hasBlock(addedHash, Optional.empty());
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
