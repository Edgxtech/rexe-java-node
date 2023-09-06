package tech.edgx.dee;

import io.ipfs.cid.Cid;
import io.ipfs.multiaddr.MultiAddress;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.peergos.client.NabuClient;
import tech.edgx.dee.util.Helpers;
import tech.edgx.dee.util.HexUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;

public class BlockRemoteTest {

    static NabuClient client0;
    static NabuClient client1;
    static NabuClient client2;

    /* Must startup a cluster first,
      use: ./start.sh 0, ./start.sh 1, ./start.sh 2
      OR// Multirun -> '3-node cluster' config from IDE
    */
    @BeforeClass
    public static void setUp() {
        try {
            // NOTE: Configs include; APIAddress(5xxx), SwarmAddresses(4xxx), gatewayAddress(808x), ProxyTgtAddress(800x)
            //       Here we need to test using the APIAddress
            MultiAddress apiAddress0 = new MultiAddress("/ip4/127.0.0.1/tcp/5000");
            MultiAddress apiAddress1 = new MultiAddress("/ip4/127.0.0.1/tcp/5001");
            MultiAddress apiAddress2 = new MultiAddress("/ip4/127.0.0.1/tcp/5002");
            client0 = new NabuClient(apiAddress0.getHost(), apiAddress0.getPort(), "/api/v0/", false);
            client1 = new NabuClient(apiAddress1.getHost(), apiAddress1.getPort(), "/api/v0/", false);
            client2 = new NabuClient(apiAddress2.getHost(), apiAddress2.getPort(), "/api/v0/", false);
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
        System.out.println("Addedhash: "+addedHash.toString());
        //Cid addedHash = Cid.decode("bafkreidqixcn3e2d3d4zdbykpiqg4eouzcvrixzry7ejm43qqzib4tuxvy");
        // Pull from same node that has it stored
        boolean has0 = client1.hasBlock(addedHash, Optional.empty());
        Assert.assertTrue("has block as expected", has0);
    }

    // Test with DP not on local net: bafkreidqixcn3e2d3d4zdbykpiqg4eouzcvrixzry7ejm43qqzib4tuxvy
    @Test
    public void putAndRemoveBlockTest() {
        try {
            String testDpName = "src/main/resources/TestDp.jar";
            File jarFile = new File(testDpName);
            Helpers.printJarInfo(jarFile);
            byte[] bytecode = Files.readAllBytes(jarFile.toPath());
            Cid addedHash = client1.putBlock(bytecode, Optional.of("raw"));
            //Cid predeployedDpHash = Cid.decode("bafkreidqixcn3e2d3d4zdbykpiqg4eouzcvrixzry7ejm43qqzib4tuxvy");

            // Pull from same node that has it stored
            boolean has0 = client1.hasBlock(addedHash, Optional.empty());
            Assert.assertTrue("has block as expected", has0);

            // Pull from a remote client
            // TODO, test if it was pulled from REMOTE
            boolean has1 = client1.hasBlock(addedHash, Optional.empty());
            Assert.assertTrue("has block as expected", has1);

            boolean bloomAdd = client1.bloomAdd(addedHash);
            Assert.assertTrue("added to bloom filter", !bloomAdd); //RamBlockstore does not filter

            byte[] data = client2.getBlock(addedHash, Optional.empty());
            print("Recovered bytecode: "+HexUtil.encodeHexString(data));
            Assert.assertTrue("block is as expected", HexUtil.encodeHexString(bytecode).equals(HexUtil.encodeHexString(data)));

            List<Cid> localRefs = client1.listBlockstore();
            Assert.assertTrue("local ref size", localRefs.size() == 1);

            client1.removeBlock(addedHash);
            List<Cid> localRefsAfter = client1.listBlockstore();
            Assert.assertTrue("local ref size after rm", localRefsAfter.size() == 0);

            boolean have = client1.hasBlock(addedHash, Optional.empty());
            Assert.assertTrue("does not have block as expected", !have);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            Assert.assertTrue("IOException", false);
        }
    }

    public void print(String msg) {
        System.out.println(msg);
    }
}
