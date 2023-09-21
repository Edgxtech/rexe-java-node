package tech.edgx.drf.chat_dp;

import io.ipfs.multiaddr.MultiAddress;
import org.junit.BeforeClass;
import tech.edgx.drf.client.DrfClient;

/* Must startup a cluster first,
  use: ./start.sh 0, ./start.sh 1, ./start.sh 2
  OR// a 'Multirun' config in IDE
*/
public class ChatDpIT {

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

    public void print(String msg) {
        System.out.println(msg);
    }
}
