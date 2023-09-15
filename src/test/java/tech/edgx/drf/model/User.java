package tech.edgx.drf.model;

import java.util.Map;

public class User {
    String username;
    String password;
    String fullname;
    String email;

    public User(String username, String password, String fullname, String email) {
        this.username = username;
        this.password = password;
        this.fullname = fullname;
        this.email = email;
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

    public static User fromJson(Map<String,Object> json) {
        return new User(
                json.get("username").toString(),
                json.get("password").toString(),
                json.get("fullname").toString(),
                json.get("email").toString());
    }
}
