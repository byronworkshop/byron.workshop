package com.byronworkshop.ui.mainactivity.adapter.pojo;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.byronworkshop.shared.pojo.Image;

import java.io.Serializable;

public class Motorcycle implements Serializable {

    private Image image;
    private String brand;
    private String model;
    private String type;
    private int cc;
    private String color;
    private String licensePlateNumber;
    private MotorcycleMetadata metadata;

    public Motorcycle() {
    }

    public Motorcycle(@Nullable Image image,
                      @NonNull String brand,
                      @NonNull String model,
                      @NonNull String type,
                      int cc,
                      @NonNull String color,
                      @NonNull String licensePlateNumber,
                      @NonNull MotorcycleMetadata metadata) {
        this.image = image;
        this.brand = brand;
        this.model = model;
        this.type = type;
        this.cc = cc;
        this.color = color;
        this.licensePlateNumber = licensePlateNumber;
        this.metadata = metadata;
    }

    public Image getImage() {
        return image;
    }

    public String getBrand() {
        return brand;
    }

    public String getModel() {
        return model;
    }

    public String getType() {
        return type;
    }

    public int getCc() {
        return cc;
    }

    public String getColor() {
        return color;
    }

    public String getLicensePlateNumber() {
        return licensePlateNumber;
    }

    public MotorcycleMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(MotorcycleMetadata metadata) {
        this.metadata = metadata;
    }
}
