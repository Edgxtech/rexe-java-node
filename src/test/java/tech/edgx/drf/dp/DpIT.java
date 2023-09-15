package tech.edgx.drf.dp;

import io.ipfs.cid.Cid;
import io.ipfs.multiaddr.MultiAddress;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import tech.edgx.drf.client.DrfClient;
import tech.edgx.drf.model.User;
import tech.edgx.drf.util.Helpers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.Optional;

/* Must startup a cluster first,
  use: ./start.sh 0, ./start.sh 1, ./start.sh 2
  OR// a 'Multirun' config in IDE
*/
public class DpIT {

    static DrfClient client0;
    static DrfClient client1;
    static DrfClient client2;

    @BeforeClass
    public static void setUp() {
        try {
            // NOTE: Configs include; APIAddress(5xxx), SwarmAddresses(4xxx), gatewayAddress(808x), ProxyTgtAddress(800x)
            MultiAddress apiAddress0 = new MultiAddress("/ip4/127.0.0.1/tcp/5000");
            MultiAddress apiAddress1 = new MultiAddress("/ip4/127.0.0.1/tcp/5001");
            MultiAddress apiAddress2 = new MultiAddress("/ip4/127.0.0.1/tcp/5002");
            client0 = new DrfClient(apiAddress0.getHost(), apiAddress0.getPort(), "/api/v0/", false);
            client1 = new DrfClient(apiAddress1.getHost(), apiAddress1.getPort(), "/api/v0/", false);
            client2 = new DrfClient(apiAddress2.getHost(), apiAddress2.getPort(), "/api/v0/", false);
        }
        catch (Exception e) {e.printStackTrace();}
    }

    @Test
    public void testCheckPutHas() throws Exception {
        String testDpName = "src/main/resources/TestDp.jar";
        File jarFile = new File(testDpName);
        Helpers.printJarInfo(jarFile);
        byte[] bytecode = Files.readAllBytes(jarFile.toPath());
        Cid addedHash = client1.putBlock(bytecode, Optional.of("raw"));
        print("Addedhash: "+addedHash.toString());
        boolean has0 = client1.hasBlock(addedHash, Optional.empty());
        Assert.assertTrue("has block as expected", has0);
    }

    @Test
    public void computeDpTestHelloWorldNoParams() {
        try {
            String testDpName = "src/main/resources/TestDp.jar";
            File jarFile = new File(testDpName);
            Helpers.printJarInfo(jarFile);
            byte[] bytecode = Files.readAllBytes(jarFile.toPath());
            Cid addedHash = client1.putBlock(bytecode, Optional.of("raw"));

            // Pull from same node it was added
            boolean has0 = client1.hasBlock(addedHash, Optional.empty());
            Assert.assertTrue("has block as expected", has0);

            Object result = client1.compute(addedHash, Optional.empty(), "getTestVal", Optional.empty());
            print("Compute result (from same client): "+result);
            Assert.assertTrue("result is as expected", result.equals("MY DP test val"));

            // Pull from other nodes, testing content routing ability
            if (client2.hasBlock(addedHash, Optional.empty()))
                client2.removeBlock(addedHash); // Remove since it might have been persisted previously
            Object result2 = client2.compute(addedHash, Optional.empty(), "getTestVal", Optional.empty());
            print("Compute result (client2): "+result2);
            Assert.assertTrue("result is as expected", result2.equals("MY DP test val"));

            if (client0.hasBlock(addedHash, Optional.empty()))
                client0.removeBlock(addedHash); // Remove since it might have been persisted previously
            Object result0 = client0.compute(addedHash, Optional.empty(), "getTestVal", Optional.empty());
            print("Compute result (client0): "+result0);
            Assert.assertTrue("result is as expected", result0.equals("MY DP test val"));

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
            Cid addedHash = client1.putBlock(bytecode, Optional.of("raw"));

            boolean has0 = client1.hasBlock(addedHash, Optional.empty());
            Assert.assertTrue("has block as expected", has0);

            Object result1 = client1.compute(addedHash, Optional.empty(), "insert", Optional.of(new String[]{TEST_USERNAME}));
            Assert.assertTrue("Insert ok", result1.toString().equals("insert: ok"));

            Object result2 = client1.compute(addedHash, Optional.empty(), "retrieve", Optional.of(new String[]{TEST_USERNAME}));
            print("DP compute result (retrieve same-client): "+result2);
            User user = User.fromJson((Map) result2);
            Assert.assertTrue("User retrieved is correct", (user.getUsername().equals(TEST_USERNAME) && user.getEmail().equals(TEST_EMAIL)));

            Object result3 = client0.compute(addedHash, Optional.empty(), "retrieve", Optional.of(new String[]{TEST_USERNAME}));
            print("DP compute result (retrieve client0): "+result3);
            User user0 = User.fromJson((Map) result3);
            Assert.assertTrue("User retrieved is correct", (user0.getUsername().equals(TEST_USERNAME) && user0.getEmail().equals(TEST_EMAIL)));

            Object result4 = client2.compute(addedHash, Optional.empty(), "retrieve", Optional.of(new String[]{TEST_USERNAME}));
            print("DP compute result (retrieve client2): "+result4);
            User user2 = User.fromJson((Map) result4);
            Assert.assertTrue("User retrieved is correct", (user2.getUsername().equals(TEST_USERNAME) && user2.getEmail().equals(TEST_EMAIL)));

            //UPDATE-RETRIEVE CLIENT0
            Object result5 = client0.compute(addedHash, Optional.empty(), "update", Optional.of(new String[]{TEST_USERNAME, TEST_NEW_EMAIL}));
            print("DP compute result (update client0): "+result5);
            Assert.assertTrue("Update ok", result5.toString().equals("update: ok"));

            Object result6 = client0.compute(addedHash, Optional.empty(), "retrieve", Optional.of(new String[]{TEST_USERNAME}));
            print("DP compute result (retrieve client0): "+result6);
            User user01 = User.fromJson((Map) result6);
            Assert.assertTrue("User retrieved is correct", (user01.getUsername().equals(TEST_USERNAME) && user01.getEmail().equals(TEST_NEW_EMAIL)));

            //UPDATE CLIENT2 - RETRIEVE CLIENT0
            Object result7 = client2.compute(addedHash, Optional.empty(), "update", Optional.of(new String[]{TEST_USERNAME, TEST_NEW_EMAIL}));
            print("DP compute result (update client2): "+result7);
            Assert.assertTrue("Update ok", result7.toString().equals("update: ok"));

            Object result8 = client0.compute(addedHash, Optional.empty(), "retrieve", Optional.of(new String[]{TEST_USERNAME}));
            print("DP compute result (retrieve client0): "+result8);
            User user02 = User.fromJson((Map) result8);
            Assert.assertTrue("User retrieved is correct", (user02.getUsername().equals(TEST_USERNAME) && user02.getEmail().equals(TEST_NEW_EMAIL)));

            Object result9 = client2.compute(addedHash, Optional.empty(), "delete", Optional.of(new String[]{TEST_USERNAME}));
            print("DP compute result (after delete): "+result9);
            Assert.assertTrue("User deleted", result9.toString().equals("delete: ok"));

        } catch (IOException ioe) {
            ioe.printStackTrace();
            Assert.assertTrue("IOException", false);
        }
    }

    @Test
    public void computeDpTestAddFunction() {
        try {
            String testDpName = "src/main/resources/TestDp.jar";
            File jarFile = new File(testDpName);
            Helpers.printJarInfo(jarFile);
            byte[] bytecode = Files.readAllBytes(jarFile.toPath());
            Cid addedHash = client1.putBlock(bytecode, Optional.of("raw"));

            // Pull from same node it was added
            boolean has0 = client1.hasBlock(addedHash, Optional.empty());
            Assert.assertTrue("has block as expected", has0);

            double val1 = 9932;
            double val2 = 343.432423;
            Object result = client1.compute(addedHash, Optional.empty(), "add", Optional.of(new String[]{String.valueOf(val1),String.valueOf(val2)}));
            print("Compute result (from same client): "+result);
            Assert.assertTrue("result is as expected", result.toString().equals(String.valueOf(val1 + val2)));

            // Pull from other nodes, testing content routing ability
            if (client2.hasBlock(addedHash, Optional.empty()))
                client2.removeBlock(addedHash); // Remove since it might have been persisted previously
            Object result2 = client2.compute(addedHash, Optional.empty(), "add", Optional.of(new String[]{String.valueOf(val1),String.valueOf(val2)}));
            print("Compute result (client2): "+result2);
            Assert.assertTrue("result is as expected", result2.toString().equals(String.valueOf(val1 + val2)));

            if (client0.hasBlock(addedHash, Optional.empty()))
                client0.removeBlock(addedHash); // Remove since it might have been persisted previously
            Object result0 = client0.compute(addedHash, Optional.empty(), "add", Optional.of(new String[]{String.valueOf(val1),String.valueOf(val2)}));
            print("Compute result (client0): "+result0);
            Assert.assertTrue("result is as expected", result0.toString().equals(String.valueOf(val1 + val2)));

        } catch (IOException ioe) {
            ioe.printStackTrace();
            Assert.assertTrue("IOException", false);
        }
    }

    public void print(String msg) {
        System.out.println(msg);
    }
}
