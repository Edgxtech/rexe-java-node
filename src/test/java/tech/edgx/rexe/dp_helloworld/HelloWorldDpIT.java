package tech.edgx.rexe.dp_helloworld;

import io.ipfs.cid.Cid;
import io.ipfs.multiaddr.MultiAddress;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import tech.edgx.rexe.client.RexeClient;
import tech.edgx.rexe.util.Helpers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;

/*
  PRE-REQS:
  1. Running cluster
    - macos: ./start.sh 0, ./start.sh 1, ./start.sh 2
    - maven: mvn exec:exec -Dinstance.id=0, mvn exec:exec -Dinstance.id=1, mvn exec:exec -Dinstance.id=2
*/
public class HelloWorldDpIT {

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
    public void testCheckPutHas() throws Exception {
        String testDpName = "dp/TestDp.jar";
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
            String testDpName = "dp/TestDp.jar";
            File jarFile = new File(testDpName);
            Helpers.printJarInfo(jarFile);
            byte[] bytecode = Files.readAllBytes(jarFile.toPath());
            Cid addedHash = client1.putBlock(bytecode, Optional.of("raw"));

            // Pull from same node it was added
            boolean has0 = client1.hasBlock(addedHash, Optional.empty());
            Assert.assertTrue("has block as expected", has0);

            Object result = client1.compute(addedHash, Optional.empty(), "tech.edgx.dp.testdp.DP:getTestVal", Optional.empty(), Optional.empty());
            print("Compute result (from same client): "+result);
            Assert.assertTrue("result is as expected", result.equals("Hello World"));

            // Pull from other nodes, testing content routing ability
            if (client2.hasBlock(addedHash, Optional.empty()))
                client2.removeBlock(addedHash); // Remove since it might have been persisted previously
            Object result2 = client2.compute(addedHash, Optional.empty(), "tech.edgx.dp.testdp.DP:getTestVal", Optional.empty(), Optional.empty());
            print("Compute result (client2): "+result2);
            Assert.assertTrue("result is as expected", result2.equals("Hello World"));

            if (client0.hasBlock(addedHash, Optional.empty()))
                client0.removeBlock(addedHash); // Remove since it might have been persisted previously
            Object result0 = client0.compute(addedHash, Optional.empty(), "tech.edgx.dp.testdp.DP:getTestVal", Optional.empty(), Optional.empty());
            print("Compute result (client0): "+result0);
            Assert.assertTrue("result is as expected", result0.equals("Hello World"));

        } catch (IOException ioe) {
            ioe.printStackTrace();
            Assert.assertTrue("IOException", false);
        }
    }

    @Test
    public void computeDpTestAddFunction() {
        try {
            String testDpName = "dp/TestDp.jar";
            File jarFile = new File(testDpName);
            Helpers.printJarInfo(jarFile);
            byte[] bytecode = Files.readAllBytes(jarFile.toPath());
            Cid addedHash = client1.putBlock(bytecode, Optional.of("raw"));

            // Pull from same node it was added
            boolean has0 = client1.hasBlock(addedHash, Optional.empty());
            Assert.assertTrue("block as expected", has0);

            double val1 = 301;
            double val2 = 343.432423;
            Object result = client1.compute(addedHash, Optional.empty(), "tech.edgx.dp.testdp.DP:add", Optional.of(new String[]{String.valueOf(val1),String.valueOf(val2)}), Optional.empty());
            print("Compute result (from same client): "+result);
            Assert.assertTrue("result as expected", result.toString().equals(String.valueOf(val1 + val2)));

            // Pull from other nodes, testing content routing ability
            if (client2.hasBlock(addedHash, Optional.empty()))
                client2.removeBlock(addedHash); // Remove since it might have been persisted previously
            Object result2 = client2.compute(addedHash, Optional.empty(), "tech.edgx.dp.testdp.DP:add", Optional.of(new String[]{String.valueOf(val1),String.valueOf(val2)}), Optional.empty());
            print("Compute result (client2): "+result2);
            Assert.assertTrue("result as expected", result2.toString().equals(String.valueOf(val1 + val2)));

            if (client0.hasBlock(addedHash, Optional.empty()))
                client0.removeBlock(addedHash); // Remove since it might have been persisted previously
            Object result0 = client0.compute(addedHash, Optional.empty(), "tech.edgx.dp.testdp.DP:add", Optional.of(new String[]{String.valueOf(val1),String.valueOf(val2)}), Optional.empty());
            print("Compute result (client0): "+result0);
            Assert.assertTrue("result as expected", result0.toString().equals(String.valueOf(val1 + val2)));

        } catch (IOException ioe) {
            ioe.printStackTrace();
            Assert.assertTrue("IOException", false);
        }
    }

    public void print(String msg) {
        System.out.println(msg);
    }
}
