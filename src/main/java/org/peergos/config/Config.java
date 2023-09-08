package org.peergos.config;

import io.ipfs.cid.Cid;
import io.ipfs.multiaddr.MultiAddress;
import io.libp2p.core.PeerId;
import io.libp2p.core.crypto.PrivKey;
import org.peergos.HostBuilder;
import org.peergos.util.JSONParser;
import org.peergos.util.JsonHelper;

import java.util.*;
import java.util.stream.Collectors;

public class Config {

    public final AddressesSection addresses;
    public final BootstrapSection bootstrap;
    public final DatastoreSection datastore;
    public final IdentitySection identity;
    public final Integer instance_id;

    public Config(Integer instance_id) {
        Config config = defaultConfig(instance_id);
        this.addresses = config.addresses;
        this.bootstrap = config.bootstrap;
        this.datastore = config.datastore;
        this.identity = config.identity;
        this.instance_id = instance_id;
    }

    private Config(AddressesSection addresses, BootstrapSection bootstrap, DatastoreSection datastore, IdentitySection identity, Integer instance_id) {
        this.addresses = addresses;
        this.bootstrap = bootstrap;
        this.datastore = datastore;
        this.identity = identity;
        this.instance_id = instance_id;
        validate(this);
    }

    public static Config build(String contents, Integer instance_id) {
        Map<String, Object> json = (Map) JSONParser.parse(contents);
        AddressesSection addressesSection = Jsonable.parse(json, p -> AddressesSection.fromJson(p));
        BootstrapSection bootstrapSection = Jsonable.parse(json, p -> BootstrapSection.fromJson(p));
        DatastoreSection datastoreSection = Jsonable.parse(json, p -> DatastoreSection.fromJson(p));
        IdentitySection identitySection = Jsonable.parse(json, p -> IdentitySection.fromJson(p));
        return new Config(addressesSection, bootstrapSection, datastoreSection, identitySection, instance_id);
    }

    @Override
    public String toString() {
        Map<String, Object> configMap = new LinkedHashMap<>();
        configMap.putAll(addresses.toJson());
        configMap.putAll(bootstrap.toJson());
        configMap.putAll(datastore.toJson());
        configMap.putAll(identity.toJson());
        return JsonHelper.pretty(configMap);
    }

    /* NOTE: Once-off; first need to create an instance-0 config by running Server with null args
    *        Need to remove bootstrap config validator and bootstrap nodes json creation */
    public Config defaultConfig(Integer instance_id) {
        if (instance_id==null) {
            instance_id=0;
        }
        HostBuilder builder = new HostBuilder().generateIdentity();
        PrivKey privKey = builder.getPrivateKey();
        PeerId peerId = builder.getPeerId();


        // ORIGINAL
//        List<MultiAddress> swarmAddresses = List.of(new MultiAddress("/ip4/0.0.0.0/tcp/4001"),
//                new MultiAddress("/ip6/::/tcp/4001"));
//        MultiAddress apiAddress = new MultiAddress("/ip4/127.0.0.1/tcp/5001");
//        MultiAddress gatewayAddress = new MultiAddress("/ip4/127.0.0.1/tcp/8080");
//        Optional<MultiAddress> proxyTargetAddress = Optional.of(new MultiAddress("/ip4/127.0.0.1/tcp/8000"));
//        Optional<String> allowTarget = Optional.of("http://localhost:8000");
        List<MultiAddress> swarmAddresses = List.of(new MultiAddress("/ip4/0.0.0.0/tcp/400"+instance_id),
                new MultiAddress("/ip6/::/tcp/400"+instance_id));
        MultiAddress apiAddress = new MultiAddress("/ip4/127.0.0.1/tcp/500"+instance_id);
        MultiAddress gatewayAddress = new MultiAddress("/ip4/127.0.0.1/tcp/808"+instance_id);
        Optional<MultiAddress> proxyTargetAddress = Optional.of(new MultiAddress("/ip4/127.0.0.1/tcp/800"+instance_id));
        Optional<String> allowTarget = Optional.of("http://localhost:800"+instance_id);

        // Original
//        List<MultiAddress> bootstrapNodes = List.of(
//                        "/dnsaddr/bootstrap.libp2p.io/p2p/QmNnooDu7bfjPFoTZYxMNLWUQJyrVwtbZg5gBMjTezGAJN",
//                        "/dnsaddr/bootstrap.libp2p.io/p2p/QmQCU2EcMqAqQPR2i9bChDtGNJchTbq5TbXJJ16u19uLTa",
//                        "/dnsaddr/bootstrap.libp2p.io/p2p/QmbLHAnMoJPWSCR5Zhtx6BHJX9KiKNN6tpvbUcqanj75Nb",
//                        "/dnsaddr/bootstrap.libp2p.io/p2p/QmcZf59bWwK5XFi76CZX8cbJ4BhTzzA3gU1ZjYZcYW3dwt",
//                        "/ip4/104.131.131.82/tcp/4001/p2p/QmaCpDMGvV2BGHeYERUEnRQAwe3N8SzbUtfsmvsqQLuvuJ", // mars.i.ipfs.io
//                        "/ip4/104.131.131.82/tcp/4001/ipfs/QmaCpDMGvV2BGHeYERUEnRQAwe3N8SzbUtfsmvsqQLuvuJ").stream()
//                .map(MultiAddress::new)
//                .collect(Collectors.toList());

        // IF this is instance0; nil bootstrap nodes (but I need to go add some data to integration)
        List<MultiAddress> bootstrapNodes = new ArrayList<>();
        if (!instance_id.equals(0)) {
            // BOOTSTRAP OFF INSTANCE 0 (PRE-GENERATED CONFIG TO GET PEER ID)
            bootstrapNodes = List.of(
                            "/ip4/127.0.0.1/tcp/4000/ipfs/12D3KooWKSc8mFAGfs8ga7VLA7evS3hRqwrk33qEg9SPDaEKFV1M").stream()
                    .map(MultiAddress::new)
                    .collect(Collectors.toList());
        }

        Map<String, Object> blockChildMap = new LinkedHashMap<>();
        blockChildMap.put("path", "blocks");
        blockChildMap.put("shardFunc", "/repo/flatfs/shard/v1/next-to-last/2");
        blockChildMap.put("sync", "true");
        blockChildMap.put("type", "flatfs");
        Mount blockMount = new Mount("/blocks", "flatfs.datastore", "measure", blockChildMap);

        Map<String, Object> dataChildMap = new LinkedHashMap<>();
        dataChildMap.put("compression", "none");
        dataChildMap.put("path", "datastore");
        dataChildMap.put("type", "h2");
        Mount rootMount = new Mount("/", "h2.datastore", "measure", dataChildMap);

        AddressesSection addressesSection = new AddressesSection(swarmAddresses, apiAddress, gatewayAddress,
                proxyTargetAddress, allowTarget);
        Filter filter = new Filter(FilterType.NONE, 0.0);
        CodecSet codecSet = CodecSet.empty();
        DatastoreSection datastoreSection = new DatastoreSection(blockMount, rootMount, filter, codecSet);
        BootstrapSection bootstrapSection = new BootstrapSection(bootstrapNodes);
        IdentitySection identity = new IdentitySection(privKey.bytes(), peerId);
        return new Config(addressesSection, bootstrapSection, datastoreSection, identity, instance_id);
    }

    public void validate(Config config) {

        if (config.addresses.getSwarmAddresses().isEmpty()) {
            throw new IllegalStateException("Expecting Addresses/Swarm entries");
        }
        // For local DEV environment, NODE0 is the genesis node doesnt require bootstrapping
        if (config.instance_id!=0 && config.bootstrap.getBootstrapAddresses().isEmpty()) {
            throw new IllegalStateException("Expecting Bootstrap addresses");
        }
        Mount blockMount = config.datastore.blockMount;
        if (!(blockMount.prefix.equals("flatfs.datastore") && blockMount.type.equals("measure"))) {
            throw new IllegalStateException("Expecting /blocks mount to have prefix == 'flatfs.datastore' and type == 'measure'");
        }
        Map<String, Object> blockParams = blockMount.getParams();
        String blockPath = (String) blockParams.get("path");
        String blockShardFunc = (String) blockParams.get("shardFunc");
        String blockType = (String) blockParams.get("type");
        if (!(blockPath.equals("blocks") && blockShardFunc.equals("/repo/flatfs/shard/v1/next-to-last/2")
                && blockType.equals("flatfs"))) {
            throw new IllegalStateException("Expecting flatfs mount at /blocks");
        }

        Mount rootMount = config.datastore.rootMount;
        if (!(rootMount.prefix.equals("h2.datastore") && rootMount.type.equals("measure"))) {
            throw new IllegalStateException("Expecting / mount to have prefix == 'h2.datastore' and type == 'measure'");
        }
        Map<String, Object> rootParams = rootMount.getParams();
        String rootPath = (String) rootParams.get("path");
        String rootCompression = (String) rootParams.get("compression");
        String rootType = (String) rootParams.get("type");
        if (!(rootPath.equals("datastore") && rootCompression.equals("none") && rootType.equals("h2"))) {
            throw new IllegalStateException("Expecting flatfs mount at /blocks");
        }
    }
}
