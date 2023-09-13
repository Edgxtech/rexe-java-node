//package tech.edgx.drf.blockstore;
//
//import io.ipfs.cid.Cid;
//import io.ipfs.multihash.Multihash;
//import org.peergos.Hash;
//import org.peergos.blockstore.Blockstore;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Optional;
//import java.util.concurrent.CompletableFuture;
//import java.util.concurrent.ConcurrentHashMap;
//
//public class DpRamBlockstore implements Blockstore {
//
//    private final ConcurrentHashMap<Cid, byte[]> blocks = new ConcurrentHashMap<>();
//
//    @Override
//    public CompletableFuture<Boolean> has(Cid c) {
//        return CompletableFuture.completedFuture(blocks.containsKey(c));
//    }
//
//    @Override
//    public CompletableFuture<Optional<byte[]>> get(Cid c) {
//        return CompletableFuture.completedFuture(Optional.ofNullable(blocks.get(c)));
//    }
//
//    @Override
//    public CompletableFuture<Cid> put(byte[] block, Cid.Codec codec) {
//        Cid cid = new Cid(1, codec, Multihash.Type.sha2_256, Hash.sha256(block));
//        blocks.put(cid, block);
//        return CompletableFuture.completedFuture(cid);
//    }
//
//    @Override
//    public CompletableFuture<Boolean> rm(Cid c) {
//        if (blocks.containsKey(c)) {
//            blocks.remove(c);
//            return CompletableFuture.completedFuture(true);
//        } else {
//            return CompletableFuture.completedFuture(false);
//        }
//    }
//
//    @Override
//    public CompletableFuture<Boolean> bloomAdd(Cid cid) {
//        //not implemented
//        return CompletableFuture.completedFuture(false);
//    }
//
//    @Override
//    public CompletableFuture<List<Cid>> refs() {
//        return CompletableFuture.completedFuture(new ArrayList(blocks.keySet()));
//    }
//}
