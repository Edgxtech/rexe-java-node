package tech.edgx.drf.chat_dp;

import com.sun.net.httpserver.HttpServer;
import io.ipfs.cid.Cid;
import io.ipfs.multiaddr.MultiAddress;
import io.ipfs.multihash.Multihash;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.runner.RunWith;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.peergos.EmbeddedIpfs;
import org.peergos.blockstore.ProvidingBlockstore;
import org.peergos.blockstore.RamBlockstore;
import org.peergos.net.APIHandler;
import org.peergos.protocol.dht.Kademlia;
import tech.edgx.drf.client.DrfClient;
import tech.edgx.drf.util.Helpers;
import tech.edgx.util.DpArgsMatcher;

import java.io.File;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.Executors;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ChatDpTest {

    static DrfClient drfClient;

    static String TEST_USERNAME_A = "drftestuserA";
    static String TEST_USERNAME_B = "drftestuserB";

//    @Mock
//    public tech.edgx.drf.dp.DP mockDp = new DP();
    // NOT SURE HOW TO MOCK THE retrieveUser() from the DP that was loaded by RuntimeService classloader
    //   A. they have generic DP.class name
    //   B. I dont use them directly, only through the runtime service

    @BeforeClass
    public static void setUp() {
        HttpServer apiServer = null;
        try {
            MultiAddress apiAddress = new MultiAddress("/ip4/127.0.0.1/tcp/8123");

            try (MockedConstruction<DrfClient> mocked = Mockito.mockConstruction(DrfClient.class,
                    withSettings().useConstructor("127.",5000,"/api/v0",true),
                    (mock, context) -> {
                        // further stubbings ...
                        when(mock.compute(any(Multihash.class), any(), anyString(), any())).thenReturn(
                                new tech.edgx.dp.chatsvc.model.User(TEST_USERNAME_A, "secretpass", TEST_USERNAME_A+" Fullname", TEST_USERNAME_A+"@test.com", "305c300d06092a864886f70d0101010500034b0030480241008dcb47244f6bd248744b7863317526d818e5c8a5347fbfc20364dbbf1698359b417813e008e72d2cf21786b366f5ce4145a717427475f625e7ab9b3c3182f6750203010001")
                        );
//                        when(mock.compute(any(Multihash.class),any(),eq("tech.edgx.dp.chatsvc.DP:retrieve"), argThat(new DpArgsMatcher(Optional.of(new String[]{TEST_USERNAME_A}))))).thenReturn(
//                                new tech.edgx.dp.chatsvc.model.User(TEST_USERNAME_A, "secretpass", TEST_USERNAME_A+" Fullname", TEST_USERNAME_A+"@test.com", "305c300d06092a864886f70d0101010500034b0030480241008dcb47244f6bd248744b7863317526d818e5c8a5347fbfc20364dbbf1698359b417813e008e72d2cf21786b366f5ce4145a717427475f625e7ab9b3c3182f6750203010001")
//                        );
//                        when(mock.compute(any(Multihash.class),any(),eq("tech.edgx.dp.chatsvc.DP:retrieve"), argThat(new DpArgsMatcher(Optional.of(new String[]{TEST_USERNAME_B}))))).thenReturn(
//                                new tech.edgx.dp.chatsvc.model.User(TEST_USERNAME_B, "secretpass", TEST_USERNAME_B+" Fullname", TEST_USERNAME_B+"@test.com", "305c300d06092a864886f70d0101010500034b003048024100d4b20b7b0d29023a43baba7a6045aa18363bfbfc992f318038109a1e7beb973440ab702dfb7b746b97a0ee7f3771359edb36c700218c0cd66de51451de1586090203010001")
//                        );
                    })) {
//                MultiAddress apiAddress = new MultiAddress("/ip4/127.0.0.1/tcp/8123");
                InetSocketAddress localAPIAddress = new InetSocketAddress(apiAddress.getHost(), apiAddress.getPort());

                apiServer = HttpServer.create(localAPIAddress, 500);
                EmbeddedIpfs ipfs = new EmbeddedIpfs(null, new ProvidingBlockstore(new RamBlockstore()), null, new Kademlia(null, false), null, Optional.empty(), Collections.emptyList());
                apiServer.createContext(APIHandler.API_URL, new APIHandler(ipfs));

                apiServer.setExecutor(Executors.newFixedThreadPool(50));
                apiServer.start();
            }

                drfClient = new DrfClient(apiAddress.getHost(), apiAddress.getPort(), "/api/v0/", false);
                String version = drfClient.version();
                //Assert.assertTrue("version", version != null);

        }
        catch (Exception e) {e.printStackTrace();}
    }

    //// TODO,
    // * this demonstrates how an app developer composes DPs
    //   As opposed to how a DP developer composes DPs (which also occur seperately)
    // call UserDP: create user -> returns users privkey (normally would manage in an app somehow)
    //         create two users
    // call ChatSvc: start chat{users[], userDpRef} -> partitions chat record, chat_user records, links users, finds users credentials in chatDP

    private MockedConstruction<DrfClient> mockAController;

//    @BeforeEach
//    public void beginTest() {
//        //create mock controller for all constructors of the given class
//        mockAController = Mockito.mockConstruction(DrfClient.class,
//                (mock, context) -> {
//                    //implement initializer for mock. Set return value for object A mock methods
//                    //this initializer will be called each time during mock creation
//                    when(mock.check()).thenReturn(" Constructor Mock A ");
//                });
//    }

    @Test
    public void testChatSvcDp() {
        String TEST_USERNAME_A = "drftestuserA";
        String TEST_USERNAME_B = "drftestuserB";
        try {

//            DrfClient drfClientSpy = spy(new DrfClient(anyString(),anyInt(),anyString(),anyBoolean()));
//            //when(drfClientSpy.compute(anyString(), anyString())).thenReturn(lcMock);
//            when(drfClientSpy.compute(any(Multihash.class),any(),eq("tech.edgx.dp.chatsvc.DP:retrieve"), argThat(new DpArgsMatcher(Optional.of(new String[]{TEST_USERNAME_A}))))).thenReturn(
//                    new tech.edgx.dp.chatsvc.model.User(TEST_USERNAME_A, "secretpass", TEST_USERNAME_A+" Fullname", TEST_USERNAME_A+"@test.com", "305c300d06092a864886f70d0101010500034b0030480241008dcb47244f6bd248744b7863317526d818e5c8a5347fbfc20364dbbf1698359b417813e008e72d2cf21786b366f5ce4145a717427475f625e7ab9b3c3182f6750203010001")
//            );
//            when(drfClientSpy.compute(any(Multihash.class),any(),eq("tech.edgx.dp.chatsvc.DP:retrieve"), argThat(new DpArgsMatcher(Optional.of(new String[]{TEST_USERNAME_B}))))).thenReturn(
//                    new tech.edgx.dp.chatsvc.model.User(TEST_USERNAME_B, "secretpass", TEST_USERNAME_B+" Fullname", TEST_USERNAME_B+"@test.com", "305c300d06092a864886f70d0101010500034b003048024100d4b20b7b0d29023a43baba7a6045aa18363bfbfc992f318038109a1e7beb973440ab702dfb7b746b97a0ee7f3771359edb36c700218c0cd66de51451de1586090203010001")
//            );

//            try (MockedConstruction<DrfClient> mockedConstruction =
//                         Mockito.mockConstruction(DrfClient.class)) {

//            try (MockedConstruction<DrfClient> mocked = Mockito.mockConstruction(DrfClient.class,
//                    withSettings().useConstructor("127.",5000,"/api/v0",true),
//                    (mock, context) -> {
//                        // further stubbings ...
//                        when(mock.compute(any(Multihash.class), any(), anyString(), any())).thenReturn(
//                            new tech.edgx.dp.chatsvc.model.User(TEST_USERNAME_A, "secretpass", TEST_USERNAME_A+" Fullname", TEST_USERNAME_A+"@test.com", "305c300d06092a864886f70d0101010500034b0030480241008dcb47244f6bd248744b7863317526d818e5c8a5347fbfc20364dbbf1698359b417813e008e72d2cf21786b366f5ce4145a717427475f625e7ab9b3c3182f6750203010001")
//                        );
////                        when(mock.compute(any(Multihash.class),any(),eq("tech.edgx.dp.chatsvc.DP:retrieve"), argThat(new DpArgsMatcher(Optional.of(new String[]{TEST_USERNAME_A}))))).thenReturn(
////                                new tech.edgx.dp.chatsvc.model.User(TEST_USERNAME_A, "secretpass", TEST_USERNAME_A+" Fullname", TEST_USERNAME_A+"@test.com", "305c300d06092a864886f70d0101010500034b0030480241008dcb47244f6bd248744b7863317526d818e5c8a5347fbfc20364dbbf1698359b417813e008e72d2cf21786b366f5ce4145a717427475f625e7ab9b3c3182f6750203010001")
////                        );
////                        when(mock.compute(any(Multihash.class),any(),eq("tech.edgx.dp.chatsvc.DP:retrieve"), argThat(new DpArgsMatcher(Optional.of(new String[]{TEST_USERNAME_B}))))).thenReturn(
////                                new tech.edgx.dp.chatsvc.model.User(TEST_USERNAME_B, "secretpass", TEST_USERNAME_B+" Fullname", TEST_USERNAME_B+"@test.com", "305c300d06092a864886f70d0101010500034b003048024100d4b20b7b0d29023a43baba7a6045aa18363bfbfc992f318038109a1e7beb973440ab702dfb7b746b97a0ee7f3771359edb36c700218c0cd66de51451de1586090203010001")
////                        );
//                    })) {

                //DrfClient drfClientSpy = new DrfClient(anyString(),anyInt(),anyString(),anyBoolean());
                //when(drfClientSpy.compute(anyString(), anyString())).thenReturn(lcMock);




            /* PRELOAD the User DP and create two new users */
            String testDpName = "src/main/resources/TestUserDp.jar";
            File jarFile = new File(testDpName);
            Helpers.printJarInfo(jarFile);
            byte[] bytecode = Files.readAllBytes(jarFile.toPath());
            print("# bytes1: "+bytecode.length);
            Cid userDpHash = drfClient.put(bytecode, Optional.of("raw"));
            print("UserDP hash, b58: "+userDpHash.toBase58()+"; "+userDpHash.toString());

            Object result1 = drfClient.compute(userDpHash, Optional.empty(), "tech.edgx.dp.usercrud.DP:create", Optional.of(new String[]{TEST_USERNAME_A}));
            print("DP compute result (create) {privkey}: " + result1);

            Object result2 = drfClient.compute(userDpHash, Optional.empty(), "tech.edgx.dp.usercrud.DP:create", Optional.of(new String[]{TEST_USERNAME_B}));
            print("DP compute result (create) {privkey}: " + result2);

            /* PRELOAD the ChatSvc DP and start a chat */
            String testChatSvcDpName = "src/main/resources/TestChatSvcDp.jar";
            File jarChatSvcFile = new File(testChatSvcDpName);
            Helpers.printJarInfo(jarChatSvcFile);
            byte[] bytecode2 = Files.readAllBytes(jarChatSvcFile.toPath());
            print("# bytes2: "+bytecode2.length);
            Cid chatSvcHash = drfClient.put(bytecode2, Optional.of("raw"));
            print("ChatSvcDp hash: "+chatSvcHash.toBase58());

            // Uses the userDp from above in the start() function IOT lookup the user details
            // TODO UP TO HERE
            //  The issue is the DP tries to create a real drf-rex-client but nil nodes running
            //  Not sure if I can mock a classloader recovered class???
            Object result3 = drfClient.compute(chatSvcHash, Optional.empty(), "tech.edgx.dp.chatsvc.DP:start", Optional.of(new String[]{TEST_USERNAME_A, TEST_USERNAME_B})); // userDpHash.toString(),
            print("DP compute result (start [chat]): " + result3);

            //}
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void print(String msg) {
        System.out.println(msg);
    }
}
