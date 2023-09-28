package tech.edgx.rexe.protocol.ebitswap;

import com.google.gson.Gson;
import com.google.protobuf.ByteString;
import io.ipfs.cid.Cid;
import io.ipfs.multihash.Multihash;
import io.libp2p.core.AddressBook;
import io.libp2p.core.PeerId;
import io.libp2p.core.Stream;
import io.libp2p.core.multiformats.Multiaddr;
import org.peergos.BlockRequestAuthoriser;
import org.peergos.Hash;
import org.peergos.HashedBlock;
import org.peergos.Want;
import org.peergos.blockstore.Blockstore;
import org.peergos.util.JSONParser;
import tech.edgx.rexe.model.dp.DpResult;
import tech.edgx.rexe.model.dp.DpWant;
import tech.edgx.rexe.protocol.ebitswap.pb.MessageOuterClass;
import tech.edgx.rexe.service.DPExecutionService;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class eBitSwapEngine {
    private static final Logger LOG = Logger.getLogger(eBitSwapEngine.class.getName());

    private final Blockstore blockStore;
    private final Set<PeerId> connections = new HashSet<>();
    private final BlockRequestAuthoriser authoriser;
    private AddressBook addressBook;

    /* filesystem specific */
    private final ConcurrentHashMap<Want, CompletableFuture<HashedBlock>> localWants = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Want, Boolean> persistBlocks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Want, PeerId> blockHaves = new ConcurrentHashMap<>();

    /* dp specific */
    private final ConcurrentHashMap<DpWant, CompletableFuture<DpResult>> localDpWants = new ConcurrentHashMap<>();
    private final DPExecutionService DPExecutionService = new DPExecutionService();

    public eBitSwapEngine(Blockstore blockStore, BlockRequestAuthoriser authoriser) {
        this.blockStore = blockStore;
        this.authoriser = authoriser;
    }

    public void setAddressBook(AddressBook addrs) {
        this.addressBook = addrs;
    }

    public synchronized void addConnection(PeerId peer, Multiaddr addr) {
        LOG.info("Adding connection and address book entry, Peer: "+peer.toString()+", addr: "+addr);
        connections.add(peer);
        addressBook.addAddrs(peer, 0, addr);
    }

    public CompletableFuture<HashedBlock> getWant(Want w, boolean addToBlockstore) {
        CompletableFuture<HashedBlock> existing = localWants.get(w);
        if (existing != null)
            return existing;
        CompletableFuture<HashedBlock> res = new CompletableFuture<>();
        if (addToBlockstore)
            persistBlocks.put(w, true);
        localWants.put(w, res);
        return res;
    }

    public CompletableFuture<DpResult> computeWant(DpWant w) {
        CompletableFuture<DpResult> existing = localDpWants.get(w);
        if (existing != null)
            return existing;
        CompletableFuture<DpResult> res = new CompletableFuture<>();
        localDpWants.put(w, res);
        return res;
    }

    public boolean hasWants() {
        return ! localWants.isEmpty();
    }

    public Set<PeerId> getConnected() {
        Set<PeerId> connected = new HashSet<>();
        synchronized (connections) {
            connected.addAll(connections);
        }
        return connected;
    }

    public Set<Want> getWants() {
        return localWants.keySet();
    }

    public Set<DpWant> getDpWants() {
        return localDpWants.keySet();
    }

    public Map<Want, PeerId> getHaves() {
        return blockHaves;
    }

    private static byte[] prefixBytes(Cid c) {
        ByteArrayOutputStream res = new ByteArrayOutputStream();
        try {
            Cid.putUvarint(res, c.version);
            Cid.putUvarint(res, c.codec.type);
            Cid.putUvarint(res, c.getType().index);
            Cid.putUvarint(res, c.getType().length);;
            return res.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void receiveMessage(MessageOuterClass.Message msg, Stream source) {
        List<MessageOuterClass.Message.BlockPresence> presences = new ArrayList<>();
        List<MessageOuterClass.Message.Block> blocks = new ArrayList<>();
        List<MessageOuterClass.Message.DpResult> dpResults = new ArrayList<>();
        List<MessageOuterClass.Message.Wantlist.Entry> wants = new ArrayList<>();

        int messageSize = 0;
        Multihash peerM = Multihash.deserialize(source.remotePeerId().getBytes());
        Cid sourcePeerId = new Cid(1, Cid.Codec.Libp2pKey, peerM.getType(), peerM.getHash());
        LOG.fine("eBitSwap received " + msg.getWantlist().getEntriesCount() + " wants, " + msg.getPayloadCount() +
                " blocks and " + msg.getResultPayloadCount() +" ComputeResults and " + msg.getBlockPresencesCount() +
                " presences from " + sourcePeerId);
        /*
           RESPOND TO WANTS
         */
        if (msg.hasWantlist()) {
            LOG.fine("Msg Wants: "+new Gson().toJson(msg.getWantlist()));
            for (MessageOuterClass.Message.Wantlist.Entry e : msg.getWantlist().getEntriesList()) {
                Cid c = Cid.cast(e.getBlock().toByteArray());
                Optional<String> auth = e.getAuth().isEmpty() ? Optional.empty() : Optional.of(e.getAuth().toStringUtf8());
                boolean isCancel = e.getCancel();
                boolean sendDontHave = e.getSendDontHave(); // The requestor wants an ack
                boolean wantBlock = e.getWantType().getNumber() == 0;
                boolean haveBlock = e.getWantType().getNumber() == 1;
                boolean wantComputation = e.getWantType().getNumber() == 2;
                if (wantBlock) {
                    Optional<byte[]> dp = blockStore.get(c).join();
                    if (dp.isPresent() && authoriser.allowRead(c, dp.get(), sourcePeerId, auth.orElse("")).join()) {
                        MessageOuterClass.Message.Block blockP = MessageOuterClass.Message.Block.newBuilder()
                                .setPrefix(ByteString.copyFrom(prefixBytes(c)))
                                .setData(ByteString.copyFrom(dp.get()))
                                .build();
                        int blockSize = blockP.getSerializedSize();
                        if (blockSize + messageSize > eBitSwap.MAX_MESSAGE_SIZE) {
                            buildAndSendMessages(wants, presences, blocks, dpResults, source::writeAndFlush);
                            wants = new ArrayList<>();
                            presences = new ArrayList<>();
                            blocks = new ArrayList<>();
                            messageSize = 0;
                        }
                        messageSize += blockSize;
                        blocks.add(blockP);
                    } else if (sendDontHave) {
                        MessageOuterClass.Message.BlockPresence presence = MessageOuterClass.Message.BlockPresence.newBuilder()
                                .setCid(ByteString.copyFrom(c.toBytes()))
                                .setType(MessageOuterClass.Message.BlockPresenceType.DontHave)
                                .build();
                        presences.add(presence);
                        messageSize += presence.getSerializedSize();
                    }
                } else if (haveBlock) {
                    boolean hasBlock = blockStore.has(c).join();
                    if (hasBlock) {
                        MessageOuterClass.Message.BlockPresence presence = MessageOuterClass.Message.BlockPresence.newBuilder()
                                .setCid(ByteString.copyFrom(c.toBytes()))
                                .setType(MessageOuterClass.Message.BlockPresenceType.Have)
                                .build();
                        presences.add(presence);
                        messageSize += presence.getSerializedSize();
                    } else if (sendDontHave) {
                        MessageOuterClass.Message.BlockPresence presence = MessageOuterClass.Message.BlockPresence.newBuilder()
                                .setCid(ByteString.copyFrom(c.toBytes()))
                                .setType(MessageOuterClass.Message.BlockPresenceType.DontHave)
                                .build();
                        presences.add(presence);
                        messageSize += presence.getSerializedSize();
                    }
                } else if (wantComputation) {
                    Optional<byte[]> dp = blockStore.get(c).join();
                    if (dp.isPresent() && authoriser.allowRead(c, dp.get(), sourcePeerId, auth.orElse("")).join()) {

                        String functionName = e.getFunctionName().toStringUtf8();
                        List<String> paramStrings = e.getParamsList().stream().map(p -> p.toStringUtf8()).collect(Collectors.toList());
                        Optional<Object[]> params = Optional.of( paramStrings.toArray(new String[0]));

//                        List<String> constructorArgs = e.getArgsList().stream().map(p -> p.toStringUtf8()).collect(Collectors.toList());
//                        Optional<Object[]> args = Optional.of( constructorArgs.toArray(new String[0]));
                        LOG.fine("Args: "+e.getArgs().toStringUtf8());
                        Optional<String> constructorArgs = !e.getArgs().isEmpty() ? Optional.of(e.getArgs().toStringUtf8()) : Optional.empty();
                        LOG.fine("Pushing compute result for functionname: "+functionName+", params: "+new Gson().toJson(paramStrings)+", constructor args: "+constructorArgs);

                        try {
                            DpResult dpResult = DPExecutionService.runDp(c, dp.get(), functionName, params, constructorArgs);
                            /* Build response in protobuf */
                            String responseString = JSONParser.toString(dpResult.result);
                            MessageOuterClass.Message.DpResult dpResultP = MessageOuterClass.Message.DpResult.newBuilder()
                                    // Instead of sending the prefix+data IOT get the CID hash, just send the CID hash
                                    .setPrefix(ByteString.copyFrom(prefixBytes(c)))
                                    .setData(ByteString.copyFrom(responseString.getBytes()))
                                    .setFunctionName(e.getFunctionName())
                                    .addAllParams(e.getParamsList())
                                    //.addAllArgs(e.getArgsList())
                                    .setArgs(e.getArgs())
                                    .setCidHash(ByteString.copyFrom(c.getHash()))
                                    .build();
                            int blockSize = dpResultP.getSerializedSize();
                            if (blockSize + messageSize > eBitSwap.MAX_MESSAGE_SIZE) {
                                buildAndSendMessages(wants, presences, blocks, dpResults, source::writeAndFlush);
                                wants = new ArrayList<>();
                                presences = new ArrayList<>();
                                blocks = new ArrayList<>();
                                dpResults = new ArrayList<>();
                                messageSize = 0;
                            }
                            messageSize += blockSize;
                            dpResults.add(dpResultP);

                        } catch (Exception ex) {
                            ex.printStackTrace();
                            LOG.severe("Exception running DP");
                        }
                    } else if (sendDontHave) {
                        MessageOuterClass.Message.BlockPresence presence = MessageOuterClass.Message.BlockPresence.newBuilder()
                                .setCid(ByteString.copyFrom(c.toBytes()))
                                .setType(MessageOuterClass.Message.BlockPresenceType.DontHave)
                                .build();
                        presences.add(presence);
                        messageSize += presence.getSerializedSize();
                    }
                }
            }
        }

        /*
            RECEIVE BLOCKS
         */
        for (MessageOuterClass.Message.Block block : msg.getPayloadList()) {
            byte[] cidPrefix = block.getPrefix().toByteArray();
            Optional<String> auth = block.getAuth().isEmpty() ?
                    Optional.empty() :
                    Optional.of(block.getAuth().toStringUtf8());
            byte[] data = block.getData().toByteArray();

            ByteArrayInputStream bin = new ByteArrayInputStream(cidPrefix);
            try {
                long version = Cid.readVarint(bin);
                Cid.Codec codec = Cid.Codec.lookup(Cid.readVarint(bin));
                Multihash.Type type = Multihash.Type.lookup((int)Cid.readVarint(bin));
                if (type != Multihash.Type.sha2_256) {
                    Logger.getGlobal().info("Unsupported hash algorithm " + type.name());
                } else {
                    byte[] hash = Hash.sha256(data);
                    Cid c = new Cid(version, codec, type, hash);
                    Want w = new Want(c, auth);
                    CompletableFuture<HashedBlock> waiter = localWants.get(w);
                    if (waiter != null) {
                        if (persistBlocks.containsKey(w)) {
                            blockStore.put(data, codec);
                            persistBlocks.remove(w);
                        }
                        waiter.complete(new HashedBlock(c, data));
                        localWants.remove(w);
                    } else
                        LOG.info("Received block we don't want: " + c);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (! localWants.isEmpty())
            LOG.info("Remaining localWants: " + localWants.size());
        for (MessageOuterClass.Message.BlockPresence blockPresence : msg.getBlockPresencesList()) {
            Cid c = Cid.cast(blockPresence.getCid().toByteArray());
            Optional<String> auth = blockPresence.getAuth().isEmpty() ? Optional.empty() : Optional.of(blockPresence.getAuth().toStringUtf8());
            Want w = new Want(c, auth);
            boolean have = blockPresence.getType().getNumber() == 0;
            if (have && localWants.containsKey(w)) {
                blockHaves.put(w, source.remotePeerId());
            }
        }

        /*
            RECEIVE COMPUTED RESULTS
         */
        for (MessageOuterClass.Message.DpResult dpResult : msg.getResultPayloadList()) {
            byte[] cidPrefix = dpResult.getPrefix().toByteArray();
            Optional<String> auth = dpResult.getAuth().isEmpty() ?
                    Optional.empty() :
                    Optional.of(dpResult.getAuth().toStringUtf8());
            byte[] data = dpResult.getData().toByteArray();
            LOG.fine("Received data: "+new String(data));

            ByteArrayInputStream bin = new ByteArrayInputStream(cidPrefix);
            try {
                long version = Cid.readVarint(bin);
                Cid.Codec codec = Cid.Codec.lookup(Cid.readVarint(bin));
                Multihash.Type type = Multihash.Type.lookup((int)Cid.readVarint(bin));
                if (type != Multihash.Type.sha2_256) {
                    Logger.getGlobal().info("Unsupported hash algorithm " + type.name());
                } else {
                    // A DIFFERENCE HERE IS THE Block/DATA is not sent, only the hash itself
                    byte[] hash = dpResult.getCidHash().toByteArray();
                    Cid c = new Cid(version, codec, type, hash);
                    // Want-Provide protocol messaging needs to send the data, function & params
                    String functionName = dpResult.getFunctionName().toStringUtf8();
                    Optional<Object[]> params = Optional.of(new Object[]{dpResult.getParamsList()});
                    //Optional<Object[]> args = Optional.of(new Object[]{dpResult.getArgsList()});
                    Optional<String> args = Optional.of(dpResult.getArgs().toStringUtf8());

                    // Match rx result with localDpWants
                    DpWant w = new DpWant(c, auth, functionName, params, args);
                    CompletableFuture<DpResult> waiter = localDpWants.get(w);
                    if (waiter != null) {
                        LOG.fine("Received requested dpresult for: " + w.cid+", "+w.functionName+", "+w.params+": "+new String(data));
                        waiter.complete(new DpResult(c, JSONParser.parse(new String(data))));
                        localDpWants.remove(w);
                    } else
                        LOG.info("Received dpresult we don't want: " + w);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (presences.isEmpty() && blocks.isEmpty() && wants.isEmpty() && dpResults.isEmpty())
            return;

        buildAndSendMessages(wants, presences, blocks, dpResults, source::writeAndFlush);
    }

    public void buildAndSendMessages(List<MessageOuterClass.Message.Wantlist.Entry> wants,
                                     List<MessageOuterClass.Message.BlockPresence> presences,
                                     List<MessageOuterClass.Message.Block> blocks,
                                     List<MessageOuterClass.Message.DpResult> dpResults,
                                     Consumer<MessageOuterClass.Message> sender) {
        // make sure we stay within the message size limit
        MessageOuterClass.Message.Builder builder = MessageOuterClass.Message.newBuilder();
        int messageSize = 0;
        for (int i=0; i < wants.size(); i++) {
            MessageOuterClass.Message.Wantlist.Entry want = wants.get(i);
            int wantSize = want.getSerializedSize();
            if (wantSize + messageSize > eBitSwap.MAX_MESSAGE_SIZE) {
                sender.accept(builder.build());
                builder = MessageOuterClass.Message.newBuilder();
                messageSize = 0;
            }
            messageSize += wantSize;
            builder = builder.setWantlist(builder.getWantlist().toBuilder().addEntries(want).build());
        }
        for (int i=0; i < presences.size(); i++) {
            MessageOuterClass.Message.BlockPresence presence = presences.get(i);
            int presenceSize = presence.getSerializedSize();
            if (presenceSize + messageSize > eBitSwap.MAX_MESSAGE_SIZE) {
                sender.accept(builder.build());
                builder = MessageOuterClass.Message.newBuilder();
                messageSize = 0;
            }
            messageSize += presenceSize;
            builder = builder.addBlockPresences(presence);
        }
        for (int i=0; i < blocks.size(); i++) {
            MessageOuterClass.Message.Block block = blocks.get(i);
            int blockSize = block.getSerializedSize();
            if (blockSize + messageSize > eBitSwap.MAX_MESSAGE_SIZE) {
                sender.accept(builder.build());
                builder = MessageOuterClass.Message.newBuilder();
                messageSize = 0;
            }
            messageSize += blockSize;
            builder = builder.addPayload(block);
        }
        for (int i=0; i < dpResults.size(); i++) {
            MessageOuterClass.Message.DpResult dpResult = dpResults.get(i);
            int blockSize = dpResult.getSerializedSize();
            if (blockSize + messageSize > eBitSwap.MAX_MESSAGE_SIZE) {
                sender.accept(builder.build());
                builder = MessageOuterClass.Message.newBuilder();
                messageSize = 0;
            }
            messageSize += blockSize;
            builder = builder.addResultPayload(dpResult);
        }
        if (messageSize > 0)
            sender.accept(builder.build());
    }
}
