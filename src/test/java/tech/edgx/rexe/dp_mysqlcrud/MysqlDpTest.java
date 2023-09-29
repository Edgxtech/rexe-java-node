package tech.edgx.rexe.dp_mysqlcrud;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpServer;
import io.ipfs.cid.Cid;
import io.ipfs.multiaddr.MultiAddress;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.peergos.EmbeddedIpfs;
import org.peergos.blockstore.ProvidingBlockstore;
import org.peergos.blockstore.RamBlockstore;
import org.peergos.net.APIHandler;
import org.peergos.protocol.dht.Kademlia;
import tech.edgx.dp.mysqlcrud.model.User;
import tech.edgx.rexe.client.RexeClient;
import tech.edgx.rexe.util.Helpers;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;

public class MysqlDpTest {

    static RexeClient rexeClient;

    @Before
    public void setUp() {
        HttpServer apiServer = null;
        try {
            MultiAddress apiAddress = new MultiAddress("/ip4/127.0.0.1/tcp/8125");
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
    public void computeDpMysqlConnector() {
        String TEST_USERNAME = "drftestuser";
        String TEST_EMAIL = "drftestuser@test.com";
        String TEST_NEW_EMAIL = "drftestuser.new@test.com";
        try {
            String testDpName = "dp/TestMysqlDp.jar";
            File jarFile = new File(testDpName);
            Helpers.printJarInfo(jarFile);
            byte[] bytecode = Files.readAllBytes(jarFile.toPath());
            Cid addedHash = rexeClient.put(bytecode, Optional.of("raw"));

            boolean has = rexeClient.hasBlock(addedHash, Optional.empty());
            Assert.assertTrue("has block as expected", has);

            Object result1 = rexeClient.compute(addedHash, Optional.empty(), "tech.edgx.dp.mysqlcrud.DP:insert", Optional.of(new String[]{TEST_USERNAME}), Optional.empty());
            print("DP compute result (insert): " + result1);
            Assert.assertTrue("Insert ok", result1.toString().equals("insert: ok"));

            // Perhaps need to provide an objectrepresentation to faciliate decoding at the other end
            //    Or just rely on the spec, I.e. the app implementing this client knows if I call retrieve on that DP it will return a User.class object of certain props
            Object result2 = rexeClient.compute(addedHash, Optional.empty(), "tech.edgx.dp.mysqlcrud.DP:retrieve", Optional.of(new String[]{TEST_USERNAME}), Optional.empty());
            print("DP compute result (retrieve): " + result2);
            User user = User.fromJson((Map) result2);
            print("Recovered user obj: " + new Gson().toJson(user));
            Assert.assertTrue("User retrieved is correct", (user.getUsername().equals(TEST_USERNAME) && user.getEmail().equals(TEST_EMAIL)));

            Object result3 = rexeClient.compute(addedHash, Optional.empty(), "tech.edgx.dp.mysqlcrud.DP:update", Optional.of(new String[]{TEST_USERNAME, TEST_NEW_EMAIL}), Optional.empty());
            print("DP compute result (on update): " + result3);
            Assert.assertTrue("Update ok", result3.toString().equals("update: ok"));

            Object result31 = rexeClient.compute(addedHash, Optional.empty(), "tech.edgx.dp.mysqlcrud.DP:retrieve", Optional.of(new String[]{TEST_USERNAME}), Optional.empty());
            print("DP compute result (after update): " + result31);
            User user2 = User.fromJson((Map) result31);
            print("Recovered user obj2: " + new Gson().toJson(user2));
            Assert.assertTrue("Email was updated", TEST_NEW_EMAIL.equals(user2.getEmail()));

            Object result4 = rexeClient.compute(addedHash, Optional.empty(), "tech.edgx.dp.mysqlcrud.DP:delete", Optional.of(new String[]{TEST_USERNAME}), Optional.empty());
            print("DP compute result (after delete): " + result4);
            Assert.assertTrue("User deleted", result4.toString().equals("delete: ok"));

        } catch (IOException ioe) {
            ioe.printStackTrace();
            Assert.assertTrue("IOException", false);
        }
    }

    public void print(String msg) {
        System.out.println(msg);
    }
}
