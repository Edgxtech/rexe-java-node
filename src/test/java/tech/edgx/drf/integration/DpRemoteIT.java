package tech.edgx.drf.integration;

import io.ipfs.cid.Cid;
import io.ipfs.multiaddr.MultiAddress;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import tech.edgx.drf.client.DrfClient;
import tech.edgx.drf.util.Helpers;
import tech.edgx.drf.util.HexUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;

public class DpRemoteIT {

    static DrfClient client0;
    static DrfClient client1;
    static DrfClient client2;

    /* Must startup a cluster first,
      use: ./start.sh 0, ./start.sh 1, ./start.sh 2
      OR// Multirun -> '3-node cluster' config from IDE
    */
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

            String result = client1.compute(addedHash, Optional.empty(), "getTestVal", Optional.empty());
            print("Compute result (from same client): "+result);
            Assert.assertTrue("result is as expected", result.equals("MY DP test val"));

            // Pull from other nodes, testing content routing ability
            if (client2.hasBlock(addedHash, Optional.empty()))
                client2.removeBlock(addedHash); // Remove since it might have been persisted previously
            String result2 = client2.compute(addedHash, Optional.empty(), "getTestVal", Optional.empty());
            print("Compute result (client2): "+result2);
            Assert.assertTrue("result is as expected", result2.equals("MY DP test val"));

            if (client0.hasBlock(addedHash, Optional.empty()))
                client0.removeBlock(addedHash); // Remove since it might have been persisted previously
            String result0 = client0.compute(addedHash, Optional.empty(), "getTestVal", Optional.empty());
            print("Compute result (client0): "+result0);
            Assert.assertTrue("result is as expected", result0.equals("MY DP test val"));

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

            double val1 = 99932;
            double val2 = 143.432423;
            String result = client1.compute(addedHash, Optional.empty(), "add", Optional.of(new String[]{String.valueOf(val1),String.valueOf(val2)}));
            print("Compute result (from same client): "+result);
            Assert.assertTrue("result is as expected", result.equals(String.valueOf(val1 + val2)));

            // Pull from other nodes, testing content routing ability
            if (client2.hasBlock(addedHash, Optional.empty()))
                client2.removeBlock(addedHash); // Remove since it might have been persisted previously
            String result2 = client2.compute(addedHash, Optional.empty(), "add", Optional.of(new String[]{String.valueOf(val1),String.valueOf(val2)}));
            print("Compute result (client2): "+result2);
            Assert.assertTrue("result is as expected", result2.equals(String.valueOf(val1 + val2)));

            if (client0.hasBlock(addedHash, Optional.empty()))
                client0.removeBlock(addedHash); // Remove since it might have been persisted previously
            String result0 = client0.compute(addedHash, Optional.empty(), "getTestVal", Optional.empty());
            print("Compute result (client0): "+result0);
            Assert.assertTrue("result is as expected", result0.equals("MY DP test val"));

        } catch (IOException ioe) {
            ioe.printStackTrace();
            Assert.assertTrue("IOException", false);
        }
    }

    public void print(String msg) {
        System.out.println(msg);
    }
}
