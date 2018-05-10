package com.byronworkshop.shared.pojo;

import java.io.Serializable;

public class Image implements Serializable {

    private String imageUrl;
    private String imagePublicUrl;
    private String thumbnailUrl;
    private String thumbnailPublicUrl;
    private String bucket;

    public Image() {
    }

    public Image(String imageUrl, String imagePublicUrl, String thumbnailUrl, String thumbnailPublicUrl, String bucket) {
        this.imageUrl = imageUrl;
        this.imagePublicUrl = imagePublicUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.thumbnailPublicUrl = thumbnailPublicUrl;
        this.bucket = bucket;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getImagePublicUrl() {
        return imagePublicUrl;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public String getThumbnailPublicUrl() {
        return thumbnailPublicUrl;
    }

    public String getBucket() {
        return bucket;
    }
}
