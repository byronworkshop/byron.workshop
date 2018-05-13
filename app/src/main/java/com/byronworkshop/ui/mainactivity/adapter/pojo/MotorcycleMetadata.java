package com.byronworkshop.ui.mainactivity.adapter.pojo;

import java.io.Serializable;
import java.util.Date;

public class MotorcycleMetadata implements Serializable {

    private Date lastWorkOrderEndDate;
    private boolean reminderEnabled;

    public MotorcycleMetadata() {

    }

    public MotorcycleMetadata(Date lastWorkOrderEndDate, boolean reminderEnabled) {
        this.lastWorkOrderEndDate = lastWorkOrderEndDate;
        this.reminderEnabled = reminderEnabled;
    }

    public Date getLastWorkOrderEndDate() {
        return lastWorkOrderEndDate;
    }

    public boolean isReminderEnabled() {
        return reminderEnabled;
    }
}
