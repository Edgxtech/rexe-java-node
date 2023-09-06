package tech.edgx.dee.model.dp;

import io.ipfs.cid.Cid;

public class DpResult {
    // Later the hash might be a way to prove the result was computed with the right DP, functionname, params
    //        It might be a hash of these values
    //        Instead of a hash of the byte[] data as in a block
    //  There may be two hashes, one for the Resource addressing of the DP,
    //  another for the Resource addressing of the computed result
    public final Cid hash;
    public final String result;

    public DpResult(Cid hash, String result) {
        this.hash = hash;
        this.result = result;
    }
}
