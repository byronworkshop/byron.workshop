package com.byronworkshop.ui.detailsactivity.adapter.pojo;

import java.io.Serializable;

public class WorkOrderMetadata implements Serializable {

    private long date;
    private int totalCost;
    private int totalReplacementsCost;
    private int totalExternalLaborCost;
    private int totalLaborCost;

    public WorkOrderMetadata() {

    }

    public WorkOrderMetadata(long date, int totalCost, int totalReplacementsCost, int totalExternalLaborCost, int totalLaborCost) {
        this.date = date;
        this.totalCost = totalCost;
        this.totalReplacementsCost = totalReplacementsCost;
        this.totalExternalLaborCost = totalExternalLaborCost;
        this.totalLaborCost = totalLaborCost;
    }

    public long getDate() {
        return date;
    }

    public int getTotalCost() {
        return totalCost;
    }

    public int getTotalReplacementsCost() {
        return totalReplacementsCost;
    }

    public int getTotalExternalLaborCost() {
        return totalExternalLaborCost;
    }

    public int getTotalLaborCost() {
        return totalLaborCost;
    }
}
