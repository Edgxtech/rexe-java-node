package tech.edgx.model.dp;

import io.ipfs.cid.Cid;

import java.util.Objects;
import java.util.Optional;

public class DpWant {
    public final Cid cid;
    public final String functionName;
    public final String[] params;
    public final Optional<String> auth;
    public DpWant(Cid cid, Optional<String> auth, String functionName, String[] params) {
        this.cid = cid;
        this.auth = auth;
        this.functionName = functionName;
        this.params = params;
    }

//    public DpWant(Cid h) {
//        this(h, Optional.empty());
//    }

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
