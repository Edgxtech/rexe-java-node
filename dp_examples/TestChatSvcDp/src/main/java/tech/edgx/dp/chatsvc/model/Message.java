package tech.edgx.dp.chatsvc.model;

import java.util.Date;

/* Must match package name and attributes used by the corresponding client */
public class Message {
    String content;
    Date dateCreated;
    String creatorUname;

    public Message(Date dateCreated, String creatorUname, String content) {
        this.dateCreated = dateCreated;
        this.creatorUname = creatorUname;
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public String getCreatorUname() {
        return creatorUname;
    }

    public void setCreatorUname(String creatorUname) {
        this.creatorUname = creatorUname;
    }
}
