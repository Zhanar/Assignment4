package gcm.play.android.samples.com.gcmquickstart.Models;

import java.io.Serializable;

/**
 * Created by Admin on 22.06.2015.
 */
public class messageInfo implements Serializable {
    public Integer getId() {
        return this.id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getText() {
        return this.text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getCreatedAt() {
        return this.createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    private Integer id;
    private String text;
    private String createdAt;
}
