package com.byronworkshop.shared.dialogs.adapters.uploadedfiles.pojo;

import android.support.annotation.Nullable;

import com.byronworkshop.shared.pojo.Image;

import java.io.Serializable;

public class UploadedImage implements Serializable {

    private long date;
    private Image image;

    public UploadedImage() {
    }

    public UploadedImage(long date, @Nullable Image image) {
        this.date = date;
        this.image = image;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public long getDate() {
        return date;
    }

    public Image getImage() {
        return image;
    }
}
