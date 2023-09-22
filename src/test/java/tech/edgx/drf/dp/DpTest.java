package tech.edgx.drf.dp;

import com.google.gson.Gson;
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
import tech.edgx.drf.client.DrfClient;
import tech.edgx.dp.mysqlcrud.model.User;
import tech.edgx.drf.util.Helpers;
import tech.edgx.drf.util.HexUtil;

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
            EmbeddedIpfs ipfs = new EmbeddedIpfs(null, new ProvidingBlockstore(new RamBlockstore()), null, new Kademlia(null, false), null, Optional.empty(), Collections.emptyList());
            apiServer.createContext(APIHandler.API_URL, new APIHandler(ipfs));

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
            String testDpName = "src/main/resources/TestDp.jar";
            File jarFile = new File(testDpName);
            Helpers.printJarInfo(jarFile);
            byte[] bytecode = Files.readAllBytes(jarFile.toPath());
            Cid addedHash = drfClient.put(bytecode, Optional.of("raw"));

            boolean has = drfClient.hasBlock(addedHash, Optional.empty());
            Assert.assertTrue("has block as expected", has);

            Object result = drfClient.compute(addedHash, Optional.empty(), "tech.edgx.dp.testdp.DP:add", Optional.of(new String[]{"1.12312","3.232432"}),Optional.empty());
            print("DP compute result: "+result);

        } catch (IOException ioe) {
            ioe.printStackTrace();
            Assert.assertTrue("IOException", false);
        }
    }

    @Test
    public void computeDpTestHelloWorldNoParams() {
        try {
            String testDpName = "src/main/resources/TestDp.jar";
            File jarFile = new File(testDpName);
            Helpers.printJarInfo(jarFile);
            byte[] bytecode = Files.readAllBytes(jarFile.toPath());
            Cid addedHash = drfClient.put(bytecode, Optional.of("raw"));

            boolean has = drfClient.hasBlock(addedHash, Optional.empty());
            Assert.assertTrue("has block as expected", has);

            Object result = drfClient.compute(addedHash, Optional.empty(), "tech.edgx.dp.testdp.DP:getTestVal", Optional.empty(), Optional.empty());
            print("DP compute result: "+result);

        } catch (IOException ioe) {
            ioe.printStackTrace();
            Assert.assertTrue("IOException", false);
        }
    }

    @Test
    public void computeDpMysqlConnector() {
        String TEST_USERNAME = "drftestuser";
        String TEST_EMAIL = "drftestuser@test.com";
        String TEST_NEW_EMAIL = "drftestuser.new@test.com";
        try {
            String testDpName = "src/main/resources/TestMysqlDp.jar";
            File jarFile = new File(testDpName);
            Helpers.printJarInfo(jarFile);
            byte[] bytecode = Files.readAllBytes(jarFile.toPath());
            Cid addedHash = drfClient.put(bytecode, Optional.of("raw"));

            boolean has = drfClient.hasBlock(addedHash, Optional.empty());
            Assert.assertTrue("has block as expected", has);

            Object result1 = drfClient.compute(addedHash, Optional.empty(), "tech.edgx.dp.mysqlcrud.DP:insert", Optional.of(new String[]{TEST_USERNAME}), Optional.empty());
            print("DP compute result (insert): "+result1);
            Assert.assertTrue("Insert ok", result1.toString().equals("insert: ok"));

            // Perhaps need to provide an objectrepresentation to faciliate decoding at the other end
            //    Or just rely on the spec, I.e. the app implementing this client knows if I call retrieve on that DP it will return a User.class object of certain props
            Object result2 = drfClient.compute(addedHash, Optional.empty(), "tech.edgx.dp.mysqlcrud.DP:retrieve", Optional.of(new String[]{TEST_USERNAME}), Optional.empty());
            print("DP compute result (retrieve): "+result2);
            User user = User.fromJson((Map) result2);
            print("Recovered user obj: "+new Gson().toJson(user));
            Assert.assertTrue("User retrieved is correct", (user.getUsername().equals(TEST_USERNAME) && user.getEmail().equals(TEST_EMAIL)));

            Object result3 = drfClient.compute(addedHash, Optional.empty(), "tech.edgx.dp.mysqlcrud.DP:update", Optional.of(new String[]{TEST_USERNAME, TEST_NEW_EMAIL}), Optional.empty());
            print("DP compute result (on update): "+result3);
            Assert.assertTrue("Update ok", result3.toString().equals("update: ok"));

            Object result31 = drfClient.compute(addedHash, Optional.empty(), "tech.edgx.dp.mysqlcrud.DP:retrieve", Optional.of(new String[]{TEST_USERNAME}), Optional.empty());
            print("DP compute result (after update): "+result31);
            User user2 = User.fromJson((Map) result31);
            print("Recovered user obj2: "+new Gson().toJson(user2));
            Assert.assertTrue("Email was updated", TEST_NEW_EMAIL.equals(user2.getEmail()));

            Object result4 = drfClient.compute(addedHash, Optional.empty(), "tech.edgx.dp.mysqlcrud.DP:delete", Optional.of(new String[]{TEST_USERNAME}), Optional.empty());
            print("DP compute result (after delete): "+result4);
            Assert.assertTrue("User deleted", result4.toString().equals("delete: ok"));

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
