import io.ipfs.multiaddr.MultiAddress;
import io.ipfs.multihash.Multihash;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import tech.edgx.dp.chatsvc.DP;
import tech.edgx.dp.chatsvc.model.Message;
import tech.edgx.dp.chatsvc.model.User;
import tech.edgx.drf.client.DrfClient;
import util.DpArgsMatcher;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


/*
  RETAINED JUST BECAUSE IT SHOWS DIFFERENT WAYS TO MOCK CLASSES
 */

public class DPTest {

    static String TEST_USERNAME_A = "dave";
    static String TEST_USERNAME_B = "bazza";

    DP dp;
//
//    MultiAddress apiAddress0 = new MultiAddress("/ip4/127.0.0.1/tcp/5000");
//
//    @Mock
//    public DrfClient drfClient = new DrfClient(apiAddress0.getHost(), apiAddress0.getPort(), "/api/v0/", false);



    @Before
    public void setUp() throws Exception {
        /// THIS WORKS FOR MOCKING THE DP CLASS ITSELF, HOWEVER PREFER TO MOCK THE DRFCLIENT
//        when(mockDp.retrieveUser(anyString(), eq(TEST_USERNAME_A))).thenReturn(
//                new User(TEST_USERNAME_A, "secretpass", TEST_USERNAME_A+" Fullname", TEST_USERNAME_A+"@test.com", "305c300d06092a864886f70d0101010500034b0030480241008dcb47244f6bd248744b7863317526d818e5c8a5347fbfc20364dbbf1698359b417813e008e72d2cf21786b366f5ce4145a717427475f625e7ab9b3c3182f6750203010001")
//        );
//        when(mockDp.retrieveUser(anyString(), eq(TEST_USERNAME_B))).thenReturn(
//                new User(TEST_USERNAME_B, "secretpass", TEST_USERNAME_B+" Fullname", TEST_USERNAME_B+"@test.com", "305c300d06092a864886f70d0101010500034b003048024100d4b20b7b0d29023a43baba7a6045aa18363bfbfc992f318038109a1e7beb973440ab702dfb7b746b97a0ee7f3771359edb36c700218c0cd66de51451de1586090203010001")
//        );
//        when(mockDp.start(anyString(), anyString(), anyString())).thenCallRealMethod();
//        when(mockDp.send(anyInt(), anyString(), anyString())).thenCallRealMethod();
//        when(mockDp.queryFeed(anyInt())).thenCallRealMethod();

        MockitoAnnotations.initMocks(this);

        DrfClient drfClient1 = mock(DrfClient.class);
        when(drfClient1.compute(any(Multihash.class),any(),eq("tech.edgx.dp.chatsvc.DP:retrieve"), argThat(new DpArgsMatcher(Optional.of(new String[]{TEST_USERNAME_A}))))).thenReturn(
                new User(TEST_USERNAME_A, "secretpass", TEST_USERNAME_A+" Fullname", TEST_USERNAME_A+"@test.com", "305c300d06092a864886f70d0101010500034b0030480241008dcb47244f6bd248744b7863317526d818e5c8a5347fbfc20364dbbf1698359b417813e008e72d2cf21786b366f5ce4145a717427475f625e7ab9b3c3182f6750203010001")
        );
        when(drfClient1.compute(any(Multihash.class),any(),eq("tech.edgx.dp.chatsvc.DP:retrieve"), argThat(new DpArgsMatcher(Optional.of(new String[]{TEST_USERNAME_B}))))).thenReturn(
                new User(TEST_USERNAME_B, "secretpass", TEST_USERNAME_B+" Fullname", TEST_USERNAME_B+"@test.com", "305c300d06092a864886f70d0101010500034b003048024100d4b20b7b0d29023a43baba7a6045aa18363bfbfc992f318038109a1e7beb973440ab702dfb7b746b97a0ee7f3771359edb36c700218c0cd66de51451de1586090203010001")
        );
        dp = new DP(drfClient1);
    }

    @Test
    public void testStart() throws Exception {
        //  THIS ALSO WORKS
//        DrfClient drfClient1 = mock(DrfClient.class);
//        System.out.println(mockingDetails(drfClient1).isMock());
//        when(drfClient1.compute(any(Multihash.class),any(),anyString(),any())).thenReturn(
//                new User(TEST_USERNAME_A, "secretpass", TEST_USERNAME_A+" Fullname", TEST_USERNAME_A+"@test.com", "305c300d06092a864886f70d0101010500034b0030480241008dcb47244f6bd248744b7863317526d818e5c8a5347fbfc20364dbbf1698359b417813e008e72d2cf21786b366f5ce4145a717427475f625e7ab9b3c3182f6750203010001")
//        );
//        DP dp = new DP(drfClient1);
        Integer chatId = dp.start("bafkreidqsvifumsanj3etycgiluhj6hkiljswxdy73thpqmkwmrla6z24a",TEST_USERNAME_A, TEST_USERNAME_B);
        System.out.println("Started chat: "+chatId);
    }

    @Test
    public void testQueryFeed() {

        Integer chatId = dp.start("<USERDPCID>", TEST_USERNAME_A, TEST_USERNAME_B);
        System.out.println("Started chat: "+chatId);
        List<Message> messages = dp.queryFeed(chatId);
        System.out.println("Result, # messages (before): "+messages.size());
        Assert.assertEquals("Messages inserted", messages.size(),0);

        /*
          If you are app developer; you may have usernames and pubkey
          // Thus this call should probably pass pubkey
          In my case, just using user_id for testing

          // TODO, potentially need to lookup user id from username
            // THIS IS WHERE PERHAPS CAN USE DRF CLIENT TO CALL THE USERS DP?
            // For the intrinsic DP test purposes cant do that here; makes more sense to do that as a DP IT in the DRF-Node s/w
            //   NOT REALLY, since as an app/DP dev I need to be able test the composition of the other DP
            //   DO I then need to add drf-node as test scope dependency; then spin up the apiserver/client

            drfClient.compute(hash, 'retrieve').getUser Id
         */
        // HERE is where the user needs to sign and/or encrypt their content using libraries in the DRFClient
        //  But this needs a shared secret key and key exchange protocol; for simplicity might just sign the message here
        // However this will be achieved in the peergos-fork not here, thus keep these tests simple
        // THIS should have: B58 CID HASH for user CRUD provider?
        // I.e. If writing an app, I might orient it around usernames,
        // but functioning requires PKI pubkeys thus in the DP it requires a way to resolve the
        // pubkey such that encryption and uniquely identifying users can be achieved
        dp.send(chatId, TEST_USERNAME_A, "Hello Baz");
        dp.send(chatId, TEST_USERNAME_B, "Hey Dave");

        messages = dp.queryFeed(chatId);
        System.out.println("Result, # messages (after): "+messages.size());
        Assert.assertEquals("Messages inserted", messages.size(),2);
    }
}
