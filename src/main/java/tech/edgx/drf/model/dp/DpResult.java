package tech.edgx.drf.model.dp;

import io.ipfs.cid.Cid;

public class DpResult {
    // Later the hash might be a way to prove the result was computed with the right DP, functionname, params
    //        It might be a hash of these values
    //        Instead of a hash of the byte[] data as in a block
    //  There may be two hashes, one for the Resource addressing of the DP,
    //  another for the Resource addressing of the computed result
    // May be able to remove Cid here, since this is used in IPFS for verifying block against the requested CID
    //    here the the block isnt returned rather the result is, thus this logic doesnt apply, CID may be pointless here
    public final Cid hash;
    public final Object result;

    public DpResult(Cid hash, Object result) {
        this.hash = hash;
        this.result = result;
    }
}
