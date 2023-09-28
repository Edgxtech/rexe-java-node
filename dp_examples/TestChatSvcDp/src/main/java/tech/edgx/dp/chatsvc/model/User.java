package tech.edgx.dp.chatsvc.model;

import com.offbynull.kademlia.Activity;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/* Must match package name and attributes used by the corresponding client */
public class User {
    String username;
    String password;
    String fullname;
    String email;
    String pubkey;

    public User(String username, String password, String fullname, String email, String pubkey) {
        this.username = username;
        this.password = password;
        this.fullname = fullname;
        this.email = email;
        this.pubkey = pubkey;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFullname() {
        return fullname;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPubkey() {
        return pubkey;
    }

    public void setPubkey(String pubkey) {
        this.pubkey = pubkey;
    }

    public static User fromJson(Map<String,Object> json) {
        return new User(
                json.get("username").toString(),
                json.get("password").toString(),
                json.get("fullname").toString(),
                json.get("email").toString(),
                json.get("pubkey").toString());
    }

    public Map toJson() {
        Map<String,Object> map = new LinkedHashMap<>();
        map.put("username", this.getUsername());
        map.put("password", this.getPassword());
        map.put("fullname", this.getFullname());
        map.put("email", this.getEmail());
        map.put("pubkey", this.getPubkey());
        return map;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final User other = (User) obj;
        if (!Objects.equals(this.email, other.email)) {
            return false;
        }
        if (!Objects.equals(this.username, other.username)) {
            return false;
        }
        if (!Objects.equals(this.pubkey, other.pubkey)) {
            return false;
        }
        if (!Objects.equals(this.fullname, other.fullname)) {
            return false;
        }
        if (!Objects.equals(this.password, other.password)) {
            return false;
        }
        return true;
    }
}
