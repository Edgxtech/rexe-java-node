package tech.edgx.drf.scratchpad;

import io.ipfs.cid.Cid;
import io.ipfs.multihash.Multihash;
import org.junit.Test;

public class ConversionTests {

    @Test
    public void convertCid() {
        System.out.println(Cid.decode("bafkreidqsvifumsanj3etycgiluhj6hkiljswxdy73thpqmkwmrla6z24a"));

        //System.out.println(Cid.decode("b2rheDkuhJcmkgriQchiydfNcta8jgVy8chXbhyV8HeC2Dju"));

    }
}
