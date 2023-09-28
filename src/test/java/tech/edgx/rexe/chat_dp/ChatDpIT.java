package tech.edgx.rexe.chat_dp;

import com.google.gson.Gson;
import io.ipfs.cid.Cid;
import io.ipfs.multiaddr.MultiAddress;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import tech.edgx.dp.chatsvc.model.Config;
import tech.edgx.dp.chatsvc.model.User;
import tech.edgx.rexe.client.RexeClient;
import tech.edgx.rexe.util.Helpers;
import tech.edgx.util.TestHelpers;

import java.io.File;
import java.nio.file.Files;
import java.util.Map;
import java.util.Optional;

/*
  WARNING: Must startup a cluster first,
  macos: ./start.sh 0, ./start.sh 1, ./start.sh 2
  maven: mvn exec:exec -Dinstance.id=0, mvn exec:exec -Dinstance.id=1, mvn exec:exec -Dinstance.id=2
*/
public class ChatDpIT {

    static RexeClient client0;
    static RexeClient client1;
    static RexeClient client2;

    @BeforeClass
    public static void setUp() {
        try {
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
    public void testProxyRetrieveUser() {
        String TEST_USERNAME_A = "drftestuserA";
        try {
            String testDpName = "dp/TestChatSvcDp.jar";
            File jarFile = new File(testDpName);
            Helpers.printJarInfo(jarFile);
            byte[] bytecode = Files.readAllBytes(jarFile.toPath());
            Cid addedHash = client0.putBlock(bytecode, Optional.of("raw"));

            boolean has0 = client0.hasBlock(addedHash, Optional.empty());
            Assert.assertTrue("block as expected", has0);

            String chatSvcConfig = TestHelpers.encodeValue(new Gson().toJson(new Config("/ip4/127.0.0.1/tcp/5002", "bafkreieurqkv5zmnokhsczemdnnj7c27qcrc6ce4lvc6rl2oerhk7a4gw4")));

            Object result3 = client0.compute(addedHash, Optional.empty(), "tech.edgx.dp.chatsvc.DP:retrieveUser", Optional.of(new String[]{TEST_USERNAME_A}), Optional.of(chatSvcConfig));
            print("DP compute result (proxy retrieve user): " + result3);
            User user = User.fromJson((Map) result3);
            Assert.assertEquals("Retrieved correct user", TEST_USERNAME_A, user.getUsername());
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testChatSvcDp() {
        String TEST_USERNAME_A = "drftestuserA";
        String TEST_USERNAME_B = "drftestuserB";
        try {
            /* PRELOAD the User DP and create two new users */
            String testDpName = "dp/TestUserDp.jar";
            File jarFile = new File(testDpName);
            Helpers.printJarInfo(jarFile);
            byte[] bytecode = Files.readAllBytes(jarFile.toPath());
            print("# bytes1: "+bytecode.length);
            Cid userDpHash = client0.put(bytecode, Optional.of("raw"));
            print("UserDP hash, b58: "+userDpHash.toBase58()+"; "+userDpHash.toString());

            Object result1 = client0.compute(userDpHash, Optional.empty(), "tech.edgx.dp.usercrud.DP:create", Optional.of(new String[]{TEST_USERNAME_A}), Optional.empty());
            print("DP compute result (create): " + result1);

            Object result2 = client0.compute(userDpHash, Optional.empty(), "tech.edgx.dp.usercrud.DP:create", Optional.of(new String[]{TEST_USERNAME_B}),Optional.empty());
            print("DP compute result (create): " + result2);

            /* PRELOAD the ChatSvc DP and start a chat */
            String testChatSvcDpName = "dp/TestChatSvcDp.jar";
            File jarChatSvcFile = new File(testChatSvcDpName);
            Helpers.printJarInfo(jarChatSvcFile);
            byte[] bytecode2 = Files.readAllBytes(jarChatSvcFile.toPath());
            print("# bytes2: "+bytecode2.length);
            Cid chatSvcHash = client0.put(bytecode2, Optional.of("raw"));
            print("ChatSvcDp hash: "+chatSvcHash.toBase58());

            // Note: Due to the design "OF THE DP" and to demonstrate composability, Can override DRF client && UserCrudDP used by this DP for subsequent calls
            String chatSvcConfig = TestHelpers.encodeValue(new Gson().toJson(new Config("/ip4/127.0.0.1/tcp/5001", userDpHash.toString())));

            Object result3 = client0.compute(chatSvcHash, Optional.empty(), "tech.edgx.dp.chatsvc.DP:start", Optional.of(new String[]{TEST_USERNAME_A, TEST_USERNAME_B}), Optional.of(chatSvcConfig));
            print("DP compute result (start [chat]): " + result3);
            Assert.assertTrue("Started chat ok", Integer.parseInt(result3.toString())>0);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    public void print(String msg) {
        System.out.println(msg);
    }
}