package tech.edgx.rexe.dp_mysqlcrud;

import io.ipfs.cid.Cid;
import io.ipfs.multiaddr.MultiAddress;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import tech.edgx.dp.mysqlcrud.model.User;
import tech.edgx.rexe.client.RexeClient;
import tech.edgx.rexe.util.Helpers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.Optional;

/*
  PRE-REQS:
  1. Running cluster
    - macos: ./start.sh 0, ./start.sh 1, ./start.sh 2
    - maven: mvn exec:exec -Dinstance.id=0, mvn exec:exec -Dinstance.id=1, mvn exec:exec -Dinstance.id=2
  2. mysql server with SQL tables; dp_examples/TestMysqlDp/SQLScript.sql
*/
public class MysqlDpIT {

    static RexeClient client0;
    static RexeClient client1;
    static RexeClient client2;

    @BeforeClass
    public static void setUp() {
        try {
            // NOTE: Configs include; APIAddress(5xxx), SwarmAddresses(4xxx), gatewayAddress(808x), ProxyTgtAddress(800x)
            MultiAddress apiAddress0 = new MultiAddress("/ip4/127.0.0.1/tcp/5000");
            MultiAddress apiAddress1 = new MultiAddress("/ip4/127.0.0.1/tcp/5001");
            MultiAddress apiAddress2 = new MultiAddress("/ip4/127.0.0.1/tcp/5002");
            client0 = new RexeClient(apiAddress0.getHost(), apiAddress0.getPort(), "/api/v0/", false);
            client1 = new RexeClient(apiAddress1.getHost(), apiAddress1.getPort(), "/api/v0/", false);
            client2 = new RexeClient(apiAddress2.getHost(), apiAddress2.getPort(), "/api/v0/", false);
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
            Cid addedHash = client1.putBlock(bytecode, Optional.of("raw"));

            boolean has0 = client1.hasBlock(addedHash, Optional.empty());
            Assert.assertTrue("has block as expected", has0);

            Object result1 = client1.compute(addedHash, Optional.empty(), "tech.edgx.dp.mysqlcrud.DP:insert", Optional.of(new String[]{TEST_USERNAME}), Optional.empty());
            Assert.assertTrue("Insert ok", result1.toString().equals("insert: ok"));

            Object result2 = client1.compute(addedHash, Optional.empty(), "tech.edgx.dp.mysqlcrud.DP:retrieve", Optional.of(new String[]{TEST_USERNAME}), Optional.empty());
            print("DP compute result (retrieve same-client): "+result2);
            User user = User.fromJson((Map) result2);
            Assert.assertTrue("User retrieved is correct", (user.getUsername().equals(TEST_USERNAME) && user.getEmail().equals(TEST_EMAIL)));

            Object result3 = client0.compute(addedHash, Optional.empty(), "tech.edgx.dp.mysqlcrud.DP:retrieve", Optional.of(new String[]{TEST_USERNAME}), Optional.empty());
            print("DP compute result (retrieve client0): "+result3);
            User user0 = User.fromJson((Map) result3);
            Assert.assertTrue("User retrieved is correct", (user0.getUsername().equals(TEST_USERNAME) && user0.getEmail().equals(TEST_EMAIL)));

            Object result4 = client2.compute(addedHash, Optional.empty(), "tech.edgx.dp.mysqlcrud.DP:retrieve", Optional.of(new String[]{TEST_USERNAME}), Optional.empty());
            print("DP compute result (retrieve client2): "+result4);
            User user2 = User.fromJson((Map) result4);
            Assert.assertTrue("User retrieved is correct", (user2.getUsername().equals(TEST_USERNAME) && user2.getEmail().equals(TEST_EMAIL)));

            //UPDATE-RETRIEVE CLIENT0
            Object result5 = client0.compute(addedHash, Optional.empty(), "tech.edgx.dp.mysqlcrud.DP:update", Optional.of(new String[]{TEST_USERNAME, TEST_NEW_EMAIL}), Optional.empty());
            print("DP compute result (update client0): "+result5);
            Assert.assertTrue("Update ok", result5.toString().equals("update: ok"));

            Object result6 = client0.compute(addedHash, Optional.empty(), "tech.edgx.dp.mysqlcrud.DP:retrieve", Optional.of(new String[]{TEST_USERNAME}), Optional.empty());
            print("DP compute result (retrieve client0): "+result6);
            User user01 = User.fromJson((Map) result6);
            Assert.assertTrue("User retrieved is correct", (user01.getUsername().equals(TEST_USERNAME) && user01.getEmail().equals(TEST_NEW_EMAIL)));

            //UPDATE CLIENT2 - RETRIEVE CLIENT0
            Object result7 = client2.compute(addedHash, Optional.empty(), "tech.edgx.dp.mysqlcrud.DP:update", Optional.of(new String[]{TEST_USERNAME, TEST_NEW_EMAIL}), Optional.empty());
            print("DP compute result (update client2): "+result7);
            Assert.assertTrue("Update ok", result7.toString().equals("update: ok"));

            Object result8 = client0.compute(addedHash, Optional.empty(), "tech.edgx.dp.mysqlcrud.DP:retrieve", Optional.of(new String[]{TEST_USERNAME}), Optional.empty());
            print("DP compute result (retrieve client0): "+result8);
            User user02 = User.fromJson((Map) result8);
            Assert.assertTrue("User retrieved is correct", (user02.getUsername().equals(TEST_USERNAME) && user02.getEmail().equals(TEST_NEW_EMAIL)));

            Object result9 = client2.compute(addedHash, Optional.empty(), "tech.edgx.dp.mysqlcrud.DP:delete", Optional.of(new String[]{TEST_USERNAME}), Optional.empty());
            print("DP compute result (after delete): "+result9);
            Assert.assertTrue("User deleted", result9.toString().equals("delete: ok"));

        } catch (IOException ioe) {
            ioe.printStackTrace();
            Assert.assertTrue("IOException", false);
        }
    }

    public void print(String msg) {
        System.out.println(msg);
    }
}
