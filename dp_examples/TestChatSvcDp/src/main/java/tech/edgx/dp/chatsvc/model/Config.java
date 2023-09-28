package tech.edgx.dp.chatsvc.model;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class Config {
    public Optional<String> apiUrl;
    public Optional<String> userDpHash;

    public Config(Optional<String> apiUrl, Optional<String> userDpHash) {
        this.apiUrl = apiUrl;
        this.userDpHash = userDpHash;
    }

    public Optional<String> getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(Optional<String> apiUrl) {
        this.apiUrl = apiUrl;
    }

    public Optional<String> getUserDpHash() {
        return userDpHash;
    }

    public void setUserDpHash(Optional<String> userDpHash) {
        this.userDpHash = userDpHash;
    }

    public Map toJson() {
        Map<String,Object> map = new LinkedHashMap<>();
        map.put("api_url", this.getApiUrl());
        map.put("user_dp_hash", this.getUserDpHash());
        return map;
    }
}
