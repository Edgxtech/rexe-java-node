package tech.edgx.rexe.chat_dp;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpServer;
import io.ipfs.cid.Cid;
import io.ipfs.multiaddr.MultiAddress;
import org.checkerframework.checker.units.qual.C;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.MockedConstruction;
import org.peergos.EmbeddedIpfs;
import org.peergos.blockstore.ProvidingBlockstore;
import org.peergos.blockstore.RamBlockstore;
import org.peergos.net.APIHandler;
import org.peergos.protocol.dht.Kademlia;
import tech.edgx.dp.chatsvc.model.Config;
import tech.edgx.rexe.client.RexeClient;
import tech.edgx.rexe.util.Helpers;
import tech.edgx.util.TestHelpers;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;

/*
  WARNING: Haven't figured out Class mocking that are dynamically loaded via the DP,
  thus this test needs running nodes as per the ITs, e.g. at least 127.0.0.1:5000
*/
public class ChatDpTest {

    static RexeClient rexeClient;

    static String TEST_USERNAME_A = "drftestuserA";
    static String TEST_USERNAME_B = "drftestuserB";

    private static MockedConstruction<RexeClient> mockAController;

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

    /*
       Demonstrates how an app developer composes DPs, as opposed to how a DP developer composes DPs (which also occurs separately)
        - call UserDP: create user -> create two users
        - call ChatSvc: start chat{users[], userDpRef} -> partitions chat record, chat_user records, links users, finds users credentials in chatDP
       WARNING: Couldn't yet figure out Class mocking that are dynamically loaded via the DP, thus this test needs running test nodes as per the ITs, e.g. at least 127.0.0.1:5000
     */
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
            Cid userDpHash = rexeClient.put(bytecode, Optional.of("raw"));
            print("UserDP hash, b58: "+userDpHash.toBase58()+"; "+userDpHash.toString());

            Object result1 = rexeClient.compute(userDpHash, Optional.empty(), "tech.edgx.dp.usercrud.DP:create", Optional.of(new String[]{TEST_USERNAME_A}), Optional.empty());
            print("DP compute result (create) {privkey}: " + result1);

            Object result2 = rexeClient.compute(userDpHash, Optional.empty(), "tech.edgx.dp.usercrud.DP:create", Optional.of(new String[]{TEST_USERNAME_B}),Optional.empty());
            print("DP compute result (create) {privkey}: " + result2);

            /* PRELOAD the ChatSvc DP and start a chat */
            String testChatSvcDpName = "dp/TestChatSvcDp.jar";
            File jarChatSvcFile = new File(testChatSvcDpName);
            Helpers.printJarInfo(jarChatSvcFile);
            byte[] bytecode2 = Files.readAllBytes(jarChatSvcFile.toPath());
            print("# bytes2: "+bytecode2.length);
            Cid chatSvcHash = rexeClient.put(bytecode2, Optional.of("raw"));
            print("ChatSvcDp hash: "+chatSvcHash.toBase58());

            String liveClientUrl = "/ip4/127.0.0.1/tcp/5000";
            print("WARNING, must have node running: "+liveClientUrl);
            String chatSvcConfig = TestHelpers.encodeValue(new Gson().toJson(new Config("/ip4/127.0.0.1/tcp/5001", userDpHash.toString())));
            print("Chat Svc Config: "+chatSvcConfig);

            Object result3 = rexeClient.compute(chatSvcHash,
                    Optional.empty(), "tech.edgx.dp.chatsvc.DP:start",
                    Optional.of(new String[]{TEST_USERNAME_A, TEST_USERNAME_B}),
                    Optional.of(chatSvcConfig));
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