package tech.edgx.protocol.dpswap;

import com.google.protobuf.ByteString;
import io.ipfs.cid.Cid;
import io.ipfs.multihash.Multihash;
import io.libp2p.core.AddressBook;
import io.libp2p.core.PeerId;
import io.libp2p.core.Stream;
import io.libp2p.core.multiformats.Multiaddr;
import org.peergos.BlockRequestAuthoriser;
import org.peergos.Hash;
import org.peergos.blockstore.Blockstore;
import tech.edgx.model.dp.DpResult;
import tech.edgx.model.dp.DpWant;
import tech.edgx.protocol.dpswap.pb.MessageOuterClass;
import tech.edgx.service.RuntimeService;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class DpswapEngine {
    private static final Logger LOG = Logger.getLogger(DpswapEngine.class.getName());

    private final Blockstore store;
    private final Set<PeerId> connections = new HashSet<>();
    private final BlockRequestAuthoriser authoriser;
    private AddressBook addressBook;

    /* filesystem specific */
    private final ConcurrentHashMap<DpWant, CompletableFuture<DpResult>> localWants = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<DpWant, Boolean> persistBlocks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<DpWant, PeerId> blockHaves = new ConcurrentHashMap<>();

    /* dp specific */
    private final ConcurrentHashMap<DpWant, CompletableFuture<DpResult>> localDpWants = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<DpWant, Boolean> persistDpResults = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<DpWant, PeerId> dpResultHaves = new ConcurrentHashMap<>();
    private final RuntimeService runtimeService = new RuntimeService();

    public DpswapEngine(Blockstore store, BlockRequestAuthoriser authoriser) {
        this.store = store;
        this.authoriser = authoriser;
    }

    public void setAddressBook(AddressBook addrs) {
        this.addressBook = addrs;
    }

    public synchronized void addConnection(PeerId peer, Multiaddr addr) {
        connections.add(peer);
        addressBook.addAddrs(peer, 0, addr);
    }

    public CompletableFuture<DpResult> getWant(DpWant w, boolean addToBlockstore) {
        CompletableFuture<DpResult> existing = localWants.get(w);
        if (existing != null)
            return existing;
        CompletableFuture<DpResult> res = new CompletableFuture<>();
        if (addToBlockstore)
            persistBlocks.put(w, true);
        localWants.put(w, res);
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

    public Set<DpWant> getWants() {
        return localWants.keySet();
    }

    public Map<DpWant, PeerId> getHaves() {
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
        List<MessageOuterClass.Message.Wantlist.Entry> wants = new ArrayList<>();

        int messageSize = 0;
        Multihash peerM = Multihash.deserialize(source.remotePeerId().getBytes());
        Cid sourcePeerId = new Cid(1, Cid.Codec.Libp2pKey, peerM.getType(), peerM.getHash());
        if (msg.hasWantlist()) {
            for (MessageOuterClass.Message.Wantlist.Entry e : msg.getWantlist().getEntriesList()) {
                Cid c = Cid.cast(e.getBlock().toByteArray());
                Optional<String> auth = e.getAuth().isEmpty() ? Optional.empty() : Optional.of(e.getAuth().toStringUtf8());
                boolean isCancel = e.getCancel();
                boolean sendDontHave = e.getSendDontHave();
                boolean wantBlock = e.getWantType().getNumber() == 0;
                if (wantBlock) {
                    Optional<byte[]> block = store.get(c).join();

                    // TODO, perform the computation here and return result instead of returning the block
                    // From the message DpWant, get the functionname and params
                    //runtimeService.runDp(cid, bytecode, functionName, params);

                    if (block.isPresent() && authoriser.allowRead(c, block.get(), sourcePeerId, auth.orElse("")).join()) {
                        MessageOuterClass.Message.Block blockP = MessageOuterClass.Message.Block.newBuilder()
                                .setPrefix(ByteString.copyFrom(prefixBytes(c)))
                                .setData(ByteString.copyFrom(block.get()))
                                .build();
                        int blockSize = blockP.getSerializedSize();
                        if (blockSize + messageSize > Dpswap.MAX_MESSAGE_SIZE) {
                            buildAndSendMessages(wants, presences, blocks, source::writeAndFlush);
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
                } else {
                    boolean hasBlock = store.has(c).join();
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
                }
            }
        }

        LOG.info("Dpswap received " + msg.getWantlist().getEntriesCount() + " wants, " + msg.getPayloadCount() +
                " blocks and " + msg.getBlockPresencesCount() + " presences from " + sourcePeerId);
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
//                int hashSize = (int)Cid.readVarint(bin);
                if (type != Multihash.Type.sha2_256) {
                    Logger.getGlobal().info("Unsupported hash algorithm " + type.name());
                } else {
                    byte[] hash = Hash.sha256(data);
                    Cid c = new Cid(version, codec, type, hash);
                    DpWant w = new DpWant(c, auth, "", null);


                    CompletableFuture<DpResult> waiter = localWants.get(w);
                    if (waiter != null) {
                        if (persistBlocks.containsKey(w)) {
                            store.put(data, codec);
                            persistBlocks.remove(w);
                        }
                        String dataString = new String(data);
                        waiter.complete(new DpResult(c, dataString));
                        localWants.remove(w);
                    } else
                        LOG.info("Received dpresult we don't want: " + c);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (! localWants.isEmpty())
            LOG.info("Remaining: " + localWants.size());
        for (MessageOuterClass.Message.BlockPresence blockPresence : msg.getBlockPresencesList()) {
            Cid c = Cid.cast(blockPresence.getCid().toByteArray());
            Optional<String> auth = blockPresence.getAuth().isEmpty() ? Optional.empty() : Optional.of(blockPresence.getAuth().toStringUtf8());
            DpWant w = new DpWant(c, auth, "", null);
            boolean have = blockPresence.getType().getNumber() == 0;
            if (have && localWants.containsKey(w)) {
                blockHaves.put(w, source.remotePeerId());
            }
        }

        if (presences.isEmpty() && blocks.isEmpty() && wants.isEmpty())
            return;

        buildAndSendMessages(wants, presences, blocks, source::writeAndFlush);
    }

    public void buildAndSendMessages(List<MessageOuterClass.Message.Wantlist.Entry> wants,
                                     List<MessageOuterClass.Message.BlockPresence> presences,
                                     List<MessageOuterClass.Message.Block> blocks,
                                     Consumer<MessageOuterClass.Message> sender) {
        // make sure we stay within the message size limit
        MessageOuterClass.Message.Builder builder = MessageOuterClass.Message.newBuilder();
        int messageSize = 0;
        for (int i=0; i < wants.size(); i++) {
            MessageOuterClass.Message.Wantlist.Entry want = wants.get(i);
            int wantSize = want.getSerializedSize();
            if (wantSize + messageSize > Dpswap.MAX_MESSAGE_SIZE) {
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
            if (presenceSize + messageSize > Dpswap.MAX_MESSAGE_SIZE) {
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
            if (blockSize + messageSize > Dpswap.MAX_MESSAGE_SIZE) {
                sender.accept(builder.build());
                builder = MessageOuterClass.Message.newBuilder();
                messageSize = 0;
            }
            messageSize += blockSize;
            builder = builder.addPayload(block);
        }
        if (messageSize > 0)
            sender.accept(builder.build());
    }
}
