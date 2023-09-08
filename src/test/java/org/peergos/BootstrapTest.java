package org.peergos;

import com.google.gson.*;
import io.ipfs.cid.Cid;
import io.ipfs.multiaddr.*;
import io.ipfs.multihash.Multihash;
import io.libp2p.core.*;
import io.libp2p.core.multiformats.Multiaddr;
import org.junit.*;
import org.peergos.blockstore.*;
import org.peergos.protocol.dht.*;

import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.stream.*;

public class BootstrapTest {

    final Gson gson = new GsonBuilder().registerTypeAdapter(String[].class, new MyDeserializer()).create();

    public static List<MultiAddress> BOOTSTRAP_NODES = List.of(
                    "/dnsaddr/bootstrap.libp2p.io/p2p/QmNnooDu7bfjPFoTZYxMNLWUQJyrVwtbZg5gBMjTezGAJN",
                    "/dnsaddr/bootstrap.libp2p.io/p2p/QmQCU2EcMqAqQPR2i9bChDtGNJchTbq5TbXJJ16u19uLTa",
                    "/dnsaddr/bootstrap.libp2p.io/p2p/QmbLHAnMoJPWSCR5Zhtx6BHJX9KiKNN6tpvbUcqanj75Nb",
                    "/dnsaddr/bootstrap.libp2p.io/p2p/QmcZf59bWwK5XFi76CZX8cbJ4BhTzzA3gU1ZjYZcYW3dwt",
                    "/ip4/104.131.131.82/tcp/4001/p2p/QmaCpDMGvV2BGHeYERUEnRQAwe3N8SzbUtfsmvsqQLuvuJ", // mars.i.ipfs.io
                    "/ip4/104.131.131.82/tcp/4001/ipfs/QmaCpDMGvV2BGHeYERUEnRQAwe3N8SzbUtfsmvsqQLuvuJ",
                    "/ip4/104.236.179.241/tcp/4001/ipfs/QmSoLPppuBtQSGwKDZT2M73ULpjvfd3aZ6ha4oFGL1KrGM",
                    "/ip4/128.199.219.111/tcp/4001/ipfs/QmSoLSafTMBsPKadTEgaXctDQVcqN88CNLHXMkTNwMKPnu",
                    "/ip4/104.236.76.40/tcp/4001/ipfs/QmSoLV4Bbm51jM9C4gDYZQ9Cy3U6aXMJDAbzgu2fzaDs64",
                    "/ip4/178.62.158.247/tcp/4001/ipfs/QmSoLer265NRgSp2LA3dPaeykiS1J6DifTC88f5uVQKNAd",
                    "/ip6/2604:a880:1:20:0:0:203:d001/tcp/4001/ipfs/QmSoLPppuBtQSGwKDZT2M73ULpjvfd3aZ6ha4oFGL1KrGM",
                    "/ip6/2400:6180:0:d0:0:0:151:6001/tcp/4001/ipfs/QmSoLSafTMBsPKadTEgaXctDQVcqN88CNLHXMkTNwMKPnu",
                    "/ip6/2604:a880:800:10:0:0:4a:5001/tcp/4001/ipfs/QmSoLV4Bbm51jM9C4gDYZQ9Cy3U6aXMJDAbzgu2fzaDs64",
                    "/ip6/2a03:b0c0:0:1010:0:0:23:1001/tcp/4001/ipfs/QmSoLer265NRgSp2LA3dPaeykiS1J6DifTC88f5uVQKNAd"
            ).stream()
            .map(MultiAddress::new)
            .collect(Collectors.toList());

    @Test
    public void bootstrap() {
        HostBuilder builder1 = HostBuilder.build(10000 + new Random().nextInt(50000),
                new RamProviderStore(), new RamRecordStore(), new RamBlockstore(), (c, b, p, a) -> CompletableFuture.completedFuture(true));
        Host node1 = builder1.build();
        node1.start().join();
        Multihash node1Id = Multihash.deserialize(node1.getPeerId().getBytes());
        System.out.println("My node id: "+node1Id.toBase58());

        try {
            Kademlia dht = builder1.getWanDht().get();
            //System.out.println("DHT (init): "+gson.toJson(dht));
            System.out.println("DHT: "+dht.getProtocolDescriptor().toString() + dht.getProtocol().toString());

            Predicate<String> bootstrapAddrFilter = addr -> !addr.contains("/wss/"); // jvm-libp2p can't parse /wss addrs
            System.out.println("Starting bootstrap routing table");
            int connections = dht.bootstrapRoutingTable(node1, BOOTSTRAP_NODES, bootstrapAddrFilter);
            System.out.println("Finshed bootstrap routing table, # connections: "+connections);
            if (connections == 0)
                throw new IllegalStateException("No connected peers!");
            System.out.println("Starting bootstrapping..");
            dht.bootstrap(node1);
            System.out.println("Finished bootstrapping");

            // INVESTIGATIONS BY TIM
            //System.out.println("DHT: "+new Gson().toJson(dht));
            Multihash block = Cid.decode("bafybeigdyrzt5sfp7udm7hu76uh7y26nf3efuylqabf3oclgtqy55fbzdi");
            System.out.println("Finding providers");
            List<PeerAddresses> providers = dht.findProviders(block, node1, 10).join();
            if (providers.isEmpty())
                throw new IllegalStateException("Couldn't find provider of block!");
            System.out.println("# Providers with test block: "+providers.size());
            System.out.println("Providers with test block: "+new Gson().toJson(providers));
            System.out.println("ResSwap Engine connected peers: "+new Gson().toJson(builder1.getResSwap().get().getResSwapEngine().getConnected()));

            // TEMPORARY, TEST RETRIEVING BLOCK
            // Add to local address book nodes known to have the block,
            // then when requesting, request from these nodes rather than whats in the engine.connections
            //node2.listenAddresses().get(0);
            //Multiaddr.fromString("/ip4/127.0.0.1/tcp/4001/p2p/" + kubo.id().get("ID"));
            //Multiaddr.fromString()
            //Multiaddress[] adresses = providers.toArray(new Multiaddress[providers.size()]);
            //Multiaddr[] addr = addrs.get(peer).join().toArray(new Multiaddr[0]);
            //providers.stream().forEach(p -> node1.getAddressBook().addAddrs(new io.libp2p.core.PeerId(p.peerId.toBytes()), 0L, p.addresses.toArray(new Multiaddr[0])));
            //Set<PeerId> providerIds = providers.stream().map(p -> new PeerId(p.peerId.toBytes())).collect(Collectors.toSet());

            // NFT: c31ZqsTr0OcmYtjRiAYJKSqDwE6sOdpFyGQxJe4Y_qo

            // ?tk=evxyDpkgvwB2XVnpFF4F0UXdJohJsOmIjd_YBLJxb5w
            // QmVkkR8CNFCTAKQYGeMJ2Az2cJ6FcX7yVN3tcBFrnSEQ8L   - is this an IPNS NAME??
            //    b/c nftstorage.link/ipfs/QmVkkR8CNFCTAKQYGeMJ2Az2cJ6FcX7yVN3tcBFrnSEQ8L
            //           > returns: https://bafybeidofyzxsfzslib5gyjt4piuwd4r3j5yepasraqdhzyo6elx2ena3u.ipfs.dweb.link/
            // bafybeidofyzxsfzslib5gyjt4piuwd4r3j5yepasraqdhzyo6elx2ena3u
            List<HashedBlock> receivedBlock = builder1.getResSwap().get().get(List.of(new Want(Cid.decode("bafybeidofyzxsfzslib5gyjt4piuwd4r3j5yepasraqdhzyo6elx2ena3u"))), node1, new HashSet<>(), false)
                    .stream()
                    .map(f -> f.join())
                    .collect(Collectors.toList());
            System.out.println("Received block: "+new String(receivedBlock.get(0).block));

            // lookup ourselves in DHT to find our nearest nodes
            List<PeerAddresses> closestPeers = dht.findClosestPeers(node1Id, 20, node1);
            if (closestPeers.size() < connections/2)
                throw new IllegalStateException("Didn't find more close peers after bootstrap: " +
                        closestPeers.size() + " < " + connections);

        } finally {
            node1.stop();
        }
    }

    public static class MyDeserializer implements JsonDeserializer<String[]> {
        @Override
        public String[] deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            System.out.println(json + ", " +typeOfT);
            String child = context.deserialize(json, String.class);
            return new String[] { child };
            //if (typeOfT.equals(Java.time))
//            if (json instanceof JsonArray) {
//                return new Gson().fromJson(json, String[].class);
//            }
//            String child = context.deserialize(json, String.class);
//            return new String[] { child };
        }
    }
}
