package tech.edgx.drf.block;

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

public class BlockIT {

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

    // Test with DP not on local net: bafkreidqixcn3e2d3d4zdbykpiqg4eouzcvrixzry7ejm43qqzib4tuxvy
    @Test
    public void putAndRemoveBlockTest() {
        try {
            String testDpName = "src/main/resources/TestDp.jar";
            File jarFile = new File(testDpName);
            Helpers.printJarInfo(jarFile);
            byte[] bytecode = Files.readAllBytes(jarFile.toPath());
            Cid addedHash = client1.putBlock(bytecode, Optional.of("raw"));

            // Pull from same node it was added
            boolean has0 = client1.hasBlock(addedHash, Optional.empty());
            Assert.assertTrue("has block as expected", has0);

            boolean bloomAdd = client1.bloomAdd(addedHash);
            Assert.assertTrue("added to bloom filter", !bloomAdd); //RamBlockstore does not filter

            byte[] data = client1.getBlock(addedHash, Optional.empty());
            print("Recovered bytecode (from same client): "+HexUtil.encodeHexString(data));
            Assert.assertTrue("block is as expected", HexUtil.encodeHexString(bytecode).equals(HexUtil.encodeHexString(data)));


            // Pull from other nodes, testing content routing ability
            if (client2.hasBlock(addedHash, Optional.empty()))
                client2.removeBlock(addedHash); // Remove since it might have been persisted previously
            byte[] data2 = client2.getBlock(addedHash, Optional.empty());
            print("Recovered bytecode (client2): "+HexUtil.encodeHexString(data2));
            Assert.assertTrue("block is as expected", HexUtil.encodeHexString(bytecode).equals(HexUtil.encodeHexString(data2)));

            if (client0.hasBlock(addedHash, Optional.empty()))
                client0.removeBlock(addedHash); // Remove since it might have been persisted previously
            byte[] data0 = client0.getBlock(addedHash, Optional.empty());
            print("Recovered bytecode (client0): "+HexUtil.encodeHexString(data0));
            Assert.assertTrue("block is as expected", HexUtil.encodeHexString(bytecode).equals(HexUtil.encodeHexString(data0)));

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
