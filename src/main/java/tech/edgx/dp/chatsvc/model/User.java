package tech.edgx.dp.chatsvc.model;

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
}
