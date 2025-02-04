package tech.edgx.rexe.client;

import io.ipfs.cid.Cid;
import io.ipfs.multiaddr.MultiAddress;
import io.ipfs.multihash.Multihash;
import org.apache.commons.lang3.StringUtils;
import org.peergos.PeerAddresses;
import org.peergos.client.Multipart;
import org.peergos.client.NamedStreamable;
import org.peergos.util.JSONParser;
import org.peergos.util.Logging;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class RexeClient extends ClientCommon {
    private static final Logger LOG = Logging.LOG();

    public RexeClient(String host, int port) {
        super(host, port);
    }

    public RexeClient(String host, int port, String version, boolean ssl) {
        super(host, port, version, DEFAULT_CONNECT_TIMEOUT_MILLIS, DEFAULT_READ_TIMEOUT_MILLIS, ssl);
    }

    public Cid put(byte[] data, Optional<String> format) throws IOException {
        String fmt = format.map(f -> "&format=" + f).orElse("");
        Multipart m = new Multipart(protocol +"://" + host + ":" + port + apiVersion+"block/put?stream-channels=true" + fmt, "UTF-8");
        try {
            m.addFilePart("file", Paths.get(""), new NamedStreamable.ByteArrayWrapper(data));
            String res = m.finish();
            LinkedHashMap<String, String> obj = (LinkedHashMap<String, String>)JSONParser.parse(res);
            LOG.fine("Data length: "+data.length+", CID: "+Cid.decode(obj.get("Hash")).toBase58());
            return Cid.decode(obj.get("Hash"));
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
        @param functionName DP function name
        @param params DP parameters array
        @param args json string of DP constructor args. Arg names must match DP definition. Json string must be url encoded
     */
    public Object compute(Multihash hash, Optional<String> auth, String functionName, Optional<Object[]> params, Optional<String> args) throws IOException {
        String authArg = auth.isPresent() ? "&auth=" + auth.get() : "";
        String fnParamsArg = params.isPresent() ? "&params=" + StringUtils.join(params.get(),",") : "";
        String constructorArgsArg = args.isPresent() ? "&args=" + args.get() : "";
        LOG.fine("Request: dp/compute?arg=" + hash + "&fn="+functionName + fnParamsArg + constructorArgsArg + authArg);
        Map<String, Object> res = retrieveMap("dp/compute?arg=" + hash + "&fn="+functionName + fnParamsArg + constructorArgsArg + authArg);
        return res.get("Result");
    }

    public List<Cid> listBlockstore() throws IOException {
        String jsonStream = new String(retrieve("refs/local"));
        return JSONParser.parseStream(jsonStream).stream()
                .map(m -> (String) (((Map) m).get("Ref")))
                .map(Cid::decode)
                .collect(Collectors.toList());
    }

    public boolean hasBlock(Multihash hash, Optional<String> auth) throws IOException {
        String authArg = auth.isPresent() ? "&auth=" + auth.get() : "";
        return "true".equals(new String(retrieve("block/has?arg=" + hash + authArg)));
    }

    public boolean bloomAdd(Multihash hash) throws IOException {
        return "true".equals(new String(retrieve("bloom/add?arg=" + hash)));
    }

    public byte[] getBlock(Multihash hash, Optional<String> auth) throws IOException {
        String authArg = auth.isPresent() ? "&auth=" + auth.get() : "";
        return retrieve("block/get?arg=" + hash + authArg); //+" &peers=12D3KooWLKbd39xXGqaViGJq4bFV5FnLufqDzXo3HZCnjV9nJca3");
    }

    public void removeBlock(Multihash hash) throws IOException {
        retrieve("block/rm?arg=" + hash);
    }

    public List<Cid> putBlocks(List<byte[]> data) throws IOException {
        return putBlocks(data, Optional.empty());
    }

    public List<Cid> putBlocks(List<byte[]> data, Optional<String> format) throws IOException {
        List<Cid> res = new ArrayList<>();
        for (byte[] value : data) {
            res.add(putBlock(value, format));
        }
        return res;
    }

    public Cid putBlock(byte[] data, Optional<String> format) throws IOException {
        String fmt = format.map(f -> "&format=" + f).orElse("");
        Multipart m = new Multipart(protocol +"://" + host + ":" + port + apiVersion+"block/put?stream-channels=true" + fmt, "UTF-8");
        try {
            m.addFilePart("file", Paths.get(""), new NamedStreamable.ByteArrayWrapper(data));
            String res = m.finish();
            LinkedHashMap<String, String> obj = (LinkedHashMap<String, String>)JSONParser.parse(res);
            return Cid.decode(obj.get("Hash"));
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public int stat(Multihash hash) throws IOException {
        Map<String, Integer> res = retrieveMap("block/stat?arg=" + hash);
        return res.get("Size");
    }

    public List<PeerAddresses> findProviders(Multihash hash) throws IOException {
        List<Map<String, Object>> results = getAndParseStream("block/dht/findprovs?arg=" + hash).stream()
                .map(x -> (Map<String, Object>) x)
                .collect(Collectors.toList());
        List<PeerAddresses> providers = new ArrayList<>();
        for (Map<String, Object> entry : results) {
            Map<String, Object> responses = (Map<String, Object>)entry.get("Responses");
            Multihash peerId = Multihash.fromBase58((String) responses.get("ID"));
            ArrayList<String> addrs = (ArrayList<String>)responses.get("Addrs");
            List<MultiAddress> peerAddresses = addrs.stream().map(a -> new MultiAddress(a)).collect(Collectors.toList());
            providers.add(new PeerAddresses(peerId, peerAddresses));
        }
        return providers;
    }
}
