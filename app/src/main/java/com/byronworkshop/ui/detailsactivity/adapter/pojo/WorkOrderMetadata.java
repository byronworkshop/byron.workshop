package com.byronworkshop.ui.detailsactivity.adapter.pojo;

import java.io.Serializable;
import java.util.Date;

public class WorkOrderMetadata implements Serializable {

    private Date startDate;
    private Date endDate;
    private int totalCost;
    private int totalReplacementsCost;
    private int totalExternalLaborCost;
    private int totalLaborCost;

    public WorkOrderMetadata() {

    }

    public WorkOrderMetadata(Date startDate,
                             Date endDate,
                             int totalCost,
                             int totalReplacementsCost,
                             int totalExternalLaborCost,
                             int totalLaborCost) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.totalCost = totalCost;
        this.totalReplacementsCost = totalReplacementsCost;
        this.totalExternalLaborCost = totalExternalLaborCost;
        this.totalLaborCost = totalLaborCost;
    }

    public Date getStartDate() {
        return startDate;
    }

    public Date getEndDate() {
        return endDate;
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
