import io.ipfs.multihash.Multihash;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import tech.edgx.dp.chatsvc.DP;
import tech.edgx.dp.chatsvc.model.Message;
import tech.edgx.dp.chatsvc.model.User;
import tech.edgx.rexe.client.RexeClient;
import util.DpArgsMatcher;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class DPTest {

    static String TEST_USERNAME_A = "dave";
    static String TEST_USERNAME_B = "bazza";

    DP dp;

    @Before
    public void setUp() throws Exception {
        /* Since this DP calls other DPs, Mock client prevents retrieval over non-existent network */
        RexeClient drfClient1 = mock(RexeClient.class);
        when(drfClient1.compute(any(Multihash.class),any(),eq("tech.edgx.dp.usercrud.DP:retrieve"), argThat(new DpArgsMatcher(Optional.of(new String[]{TEST_USERNAME_A}))), any())).thenReturn(
                (new User(TEST_USERNAME_A, "secretpass", TEST_USERNAME_A+" Fullname", TEST_USERNAME_A+"@test.com", "305c300d06092a864886f70d0101010500034b0030480241008dcb47244f6bd248744b7863317526d818e5c8a5347fbfc20364dbbf1698359b417813e008e72d2cf21786b366f5ce4145a717427475f625e7ab9b3c3182f6750203010001")).toJson()
        );
        when(drfClient1.compute(any(Multihash.class),any(),eq("tech.edgx.dp.usercrud.DP:retrieve"), argThat(new DpArgsMatcher(Optional.of(new String[]{TEST_USERNAME_B}))), any())).thenReturn(
                new User(TEST_USERNAME_B, "secretpass", TEST_USERNAME_B+" Fullname", TEST_USERNAME_B+"@test.com", "305c300d06092a864886f70d0101010500034b003048024100d4b20b7b0d29023a43baba7a6045aa18363bfbfc992f318038109a1e7beb973440ab702dfb7b746b97a0ee7f3771359edb36c700218c0cd66de51451de1586090203010001").toJson()
        );
        dp = new DP();
        dp.overrideRexeClient(drfClient1);
    }

    @Test
    public void testStart() throws Exception {
        Integer chatId = dp.start(TEST_USERNAME_A, TEST_USERNAME_B);
        System.out.println("Started chat: "+chatId);
    }

    @Test
    public void testQueryFeed() {
        Integer chatId = dp.start(TEST_USERNAME_A, TEST_USERNAME_B);
        System.out.println("Started chat: "+chatId);
        List<Message> messages = dp.queryFeed(chatId);
        System.out.println("Result, # messages (before): "+messages.size());
        Assert.assertEquals("Messages inserted", messages.size(),0);

        // A final implementation will use pki and covered comms
        dp.send(chatId, TEST_USERNAME_A, "Hello Baz");
        dp.send(chatId, TEST_USERNAME_B, "Hey Dave");

        messages = dp.queryFeed(chatId);
        System.out.println("Result, # messages (after): "+messages.size());
        Assert.assertEquals("Messages inserted", messages.size(),2);
    }
}
