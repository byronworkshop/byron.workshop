package com.byronworkshop.shared.dialogs.adapters.uploadedfiles.pojo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.byronworkshop.shared.pojo.Image;

import java.io.Serializable;
import java.util.Date;

public class UploadedImage implements Serializable {

    private Date date;
    private Image image;

    public UploadedImage() {
    }

    public UploadedImage(@NonNull Date date, @Nullable Image image) {
        this.date = date;
        this.image = image;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Date getDate() {
        return date;
    }

    public Image getImage() {
        return image;
    }
}
