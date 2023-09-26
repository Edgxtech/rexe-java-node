package tech.edgx.rexe.model.dp;

import io.ipfs.cid.Cid;

public class DpResult {
    // Later the hash may prove the result was computed with the right DP, function, params rather than a hash of the payload data
    // Two hashes, one for Resource addressing of the DP, another for Resource addressing of the computed result
    // Otherwise can remove Cid here
    public final Cid hash;
    public final Object result;

    public DpResult(Cid hash, Object result) {
        this.hash = hash;
        this.result = result;
    }
}
