package tech.edgx.rexe.model.dp;

import io.ipfs.cid.Cid;

import java.util.Objects;
import java.util.Optional;

public class DpWant {
    public final Cid cid;
    public final String functionName;
    /* Function params */
    public final Optional<Object[]> params;
    /* Constructor args */
    //public final Optional<Object[]> args;
    public final Optional<String> args;
    public final Optional<String> auth;

    public DpWant(Cid cid, Optional<String> auth, String functionName, Optional<Object[]> params, Optional<String> args) { //Optional<Object[]> args
        this.cid = cid;
        this.auth = auth;
        this.functionName = functionName;
        this.params = params;
        this.args = args;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DpWant want = (DpWant) o;
        return cid.equals(want.cid) && auth.equals(want.auth);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cid, auth);
    }
}
