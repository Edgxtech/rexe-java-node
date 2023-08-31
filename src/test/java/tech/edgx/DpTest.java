package tech.edgx;

import com.sun.net.httpserver.HttpServer;
import io.ipfs.api.cbor.CborObject;
import io.ipfs.cid.Cid;
import io.ipfs.multiaddr.MultiAddress;
import io.libp2p.core.Host;
import io.libp2p.core.PeerId;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.peergos.*;
import org.peergos.blockstore.Blockstore;
import org.peergos.blockstore.RamBlockstore;
import org.peergos.blockstore.TypeLimitedBlockstore;
import org.peergos.client.NabuClient;
import org.peergos.net.APIHandler;
import org.peergos.protocol.dht.Kademlia;
import org.peergos.protocol.dht.RamProviderStore;
import org.peergos.protocol.dht.RamRecordStore;
import tech.edgx.service.DpswapDpResultService;
import tech.edgx.util.Helpers;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.function.Predicate;

public class DpTest {

    @Test
    public void putAndRemoveDpTest() {
        HttpServer apiServer = null;
        try {
            MultiAddress apiAddress = new MultiAddress("/ip4/127.0.0.1/tcp/8123");
            InetSocketAddress localAPIAddress = new InetSocketAddress(apiAddress.getHost(), apiAddress.getPort());

            apiServer = HttpServer.create(localAPIAddress, 500);
            APIService service = new APIService(new RamBlockstore(), new BitswapBlockService(null, null), null, new DpswapDpResultService(null,null), new RamBlockstore());
            apiServer.createContext(APIService.API_URL, new APIHandler(service, null));
            apiServer.setExecutor(Executors.newFixedThreadPool(50));
            apiServer.start();

            NabuClient nabu = new NabuClient(apiAddress.getHost(), apiAddress.getPort(), "/api/v0/", false);
            String version = nabu.version();
            Assert.assertTrue("version", version != null);

//            String testDpName = "src/main/resources/TestDp.jar";
//            File jarFile = new File(testDpName);
//            Helpers.printJarInfo(jarFile);
//            //byte[] block = text.getBytes();
//            byte[] bytecode = Files.readAllBytes(jarFile.toPath());
//            Cid addedHash = nabu.putDp(bytecode, Optional.of("raw"));

            String text = "Hello world!";
            byte[] block = text.getBytes();
            Cid addedHash = nabu.putDp(block, Optional.of("raw"));

            /// UP TO HERE; add methods in the Client to get details of DPs
            /// B/C it goes into a DP specific STORE, these requests wont work / wont find the block

            int size  = nabu.stat(addedHash);
            print("Size: "+size);

            boolean has = nabu.hasBlock(addedHash, Optional.empty());
            Assert.assertTrue("has block as expected", has);

            boolean bloomAdd = nabu.bloomAdd(addedHash);
            Assert.assertTrue("added to bloom filter", !bloomAdd); //RamBlockstore does not filter

            byte[] data = nabu.getBlock(addedHash, Optional.empty());
            // TODO, convert back to JAR and compare it to original
            //Assert.assertTrue("block is as expected", text.equals(new String(data)));

            List<Cid> localRefs = nabu.listBlockstore();
            Assert.assertTrue("local ref size", localRefs.size() == 1);

            nabu.removeBlock(addedHash);
            List<Cid> localRefsAfter = nabu.listBlockstore();
            Assert.assertTrue("local ref size after rm", localRefsAfter.size() == 0);

            boolean have = nabu.hasBlock(addedHash, Optional.empty());
            Assert.assertTrue("does not have block as expected", !have);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            Assert.assertTrue("IOException", false);
        } finally {
            if (apiServer != null) {
                apiServer.stop(1);
            }
        }
    }

    public void print(String msg) {
        System.out.println(msg);
    }
}
