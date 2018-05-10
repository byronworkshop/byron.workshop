package com.byronworkshop.shared.dialogs.adapters.workordercost.pojo;

import android.support.annotation.NonNull;

import java.io.Serializable;

public class Cost implements Serializable {

    public static final String TYPE_REPLACEMENT = "1_replacement";
    public static final String TYPE_EXTERNAL = "2_external";
    public static final String TYPE_LABOR_COST = "3_labor_cost";

    private String type;
    private String description;
    private int amount;

    public Cost() {

    }

    public Cost(@NonNull String type, @NonNull String description, int amount) {
        this.type = type;
        this.description = description;
        this.amount = amount;
    }

    public String getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public int getAmount() {
        return amount;
    }
}
