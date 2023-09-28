package tech.edgx.dp.chatsvc.model;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class Config {
    /* names must match DP constructor spec */
    public String apiUrl;
    public String userDpHash;

    public Config(String apiUrl, String userDpHash) {
        this.apiUrl = apiUrl;
        this.userDpHash = userDpHash;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public String getUserDpHash() {
        return userDpHash;
    }

    public void setUserDpHash(String userDpHash) {
        this.userDpHash = userDpHash;
    }

    public Map toJson() {
        Map<String,Object> map = new LinkedHashMap<>();
        map.put("api_url", this.getApiUrl());
        map.put("user_dp_hash", this.getUserDpHash());
        return map;
    }
}
