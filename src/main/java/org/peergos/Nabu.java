package org.peergos;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpServer;
import io.ipfs.multiaddr.MultiAddress;
import org.peergos.config.*;
import org.peergos.net.APIHandler;
import org.peergos.net.HttpProxyHandler;
import org.peergos.protocol.http.*;
import org.peergos.util.JSONParser;
import org.peergos.util.JsonHelper;
import org.peergos.util.Logging;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.peergos.EmbeddedIpfs.buildBlockStore;

public class Nabu {

    private static final Logger LOG = Logger.getLogger(Nabu.class.getName());

    private static HttpProtocol.HttpRequestProcessor proxyHandler(MultiAddress target) {
        return (s, req, h) -> HttpProtocol.proxyRequest(req, new InetSocketAddress(target.getHost(), target.getPort()), h);
    }

    public Nabu(Args args) throws Exception {
        Optional<Integer> instance_id = args.hasArg("instance_id") ? Optional.of(args.getInt("instance_id")) : Optional.empty();
        LOG.info("Booting up with instanceid: "+instance_id);
        Path ipfsPath = getIPFSPath(args, instance_id);
        LOG.info("Booting up with ipfs path: "+ipfsPath);

        Logging.init(ipfsPath, args.getBoolean("logToConsole", false));
        Config config = readConfig(ipfsPath, args, instance_id);
        if (config.metrics.enabled) {
            AggregatedMetrics.startExporter(config.metrics.address, config.metrics.port);
        }
        LOG.info("Starting Nabu version: " + APIHandler.CURRENT_VERSION);
        BlockRequestAuthoriser authoriser = (c, b, p, a) -> CompletableFuture.completedFuture(true);

        EmbeddedIpfs ipfs = EmbeddedIpfs.build(ipfsPath,
                buildBlockStore(config, ipfsPath),
                config.addresses.getSwarmAddresses(),
                config.bootstrap.getBootstrapAddresses(),
                config.identity,
                authoriser,
                config.addresses.proxyTargetAddress.map(Nabu::proxyHandler)
        );
        ipfs.start();
        String apiAddressArg = "Addresses.API";
        MultiAddress apiAddress = args.hasArg(apiAddressArg) ? new MultiAddress(args.getArg(apiAddressArg)) :  config.addresses.apiAddress;
        LOG.info("Api address: "+apiAddress);
        InetSocketAddress localAPIAddress = new InetSocketAddress(apiAddress.getHost(), apiAddress.getPort());

        int maxConnectionQueue = 500;
        int handlerThreads = 50;
        LOG.info("Starting RPC API server at " + apiAddress.getHost() + ":" + localAPIAddress.getPort());
        HttpServer apiServer = HttpServer.create(localAPIAddress, maxConnectionQueue);

        apiServer.createContext(APIHandler.API_URL, new APIHandler(ipfs));
        if (config.addresses.proxyTargetAddress.isPresent())
            apiServer.createContext(HttpProxyService.API_URL, new HttpProxyHandler(new HttpProxyService(ipfs.node, ipfs.p2pHttp.get(), ipfs.dht)));
        apiServer.setExecutor(Executors.newFixedThreadPool(handlerThreads));
        apiServer.start();

        Thread shutdownHook = new Thread(() -> {
            LOG.info("Stopping server...");
            try {
                ipfs.stop().join();
                apiServer.stop(3); //wait max 3 seconds
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    private Path getIPFSPath(Args args, Optional<Integer> instance_id) {
        Optional<String> ipfsPath = args.getOptionalArg("IPFS_PATH");
        if (ipfsPath.isEmpty()) {
            String home = args.getArg("HOME");
            Path defaultIpfsPath;
            if (instance_id.isPresent()) {
                defaultIpfsPath = Path.of(home, ".ipfs" + instance_id.get());
            } else {
                defaultIpfsPath = Path.of(home, ".ipfs");
            }
            return defaultIpfsPath;
        }
        return Path.of(ipfsPath.get());
    }

    private Config readConfig(Path configPath, Args args, Optional<Integer> instance_id) throws IOException {
        Path configFilePath = configPath.resolve("config");
        File configFile = configFilePath.toFile();
        LOG.info("Config exists: "+configFile.exists());
        if (!configFile.exists()) {
            LOG.info("Unable to find config file. Creating default config");
            Optional<String> s3datastoreArgs = args.getOptionalArg("s3.datastore");
            Config config = null;
            if (s3datastoreArgs.isPresent()) {
                Map<String, Object> json = (Map) JSONParser.parse(s3datastoreArgs.get());
                Map<String, Object> blockChildMap = new LinkedHashMap<>();
                blockChildMap.put("region", JsonHelper.getStringProperty(json,"region"));
                blockChildMap.put("bucket", JsonHelper.getStringProperty(json,"bucket"));
                blockChildMap.put("rootDirectory", JsonHelper.getStringProperty(json,"rootDirectory"));
                blockChildMap.put("regionEndpoint", JsonHelper.getStringProperty(json,"regionEndpoint"));
                if (JsonHelper.getOptionalProperty(json,"accessKey").isPresent()) {
                    blockChildMap.put("accessKey", JsonHelper.getStringProperty(json, "accessKey"));
                }
                if (JsonHelper.getOptionalProperty(json,"secretKey").isPresent()) {
                    blockChildMap.put("secretKey", JsonHelper.getStringProperty(json, "secretKey"));
                }
                blockChildMap.put("type", "s3ds");
                Mount s3BlockMount = new Mount("/blocks", "s3.datastore", "measure", blockChildMap);
                config = new Config(() -> s3BlockMount, instance_id);
            } else {
                config = new Config(instance_id);
            }
            Files.write(configFilePath, config.toString().getBytes(), StandardOpenOption.CREATE);
            return config;
        }
        return Config.build(Files.readString(configFilePath), instance_id);
    }

    public static void main(String[] args) {
        try {
            System.out.println("Args: "+new Gson().toJson(args));
            new Nabu(Args.parse(args));
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "SHUTDOWN", e);
        }
    }
}