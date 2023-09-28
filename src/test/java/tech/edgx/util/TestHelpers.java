package tech.edgx.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class TestHelpers {

    public static String encodeValue(String value) throws Exception {
        return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
    }
}
