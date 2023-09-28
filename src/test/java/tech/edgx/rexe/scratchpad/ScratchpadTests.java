package tech.edgx.rexe.scratchpad;

import com.google.gson.Gson;
import org.junit.Test;

import java.util.Map;

public class ScratchpadTests {

    @Test
    public void convertJsonToMap() {
        String json = "{a:b, 1:2}";
        Map<String,Object> map = new Gson().fromJson(json, Map.class);
        System.out.println(map);
    }


}
