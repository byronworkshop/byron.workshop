package com.byronworkshop.ui.mainactivity.adapter.pojo;

import java.io.Serializable;

public class MotorcycleMetadata implements Serializable {

    private long lastWorkOrderDate;
    private boolean reminderEnabled;

    public MotorcycleMetadata() {

    }

    public MotorcycleMetadata(long lastWorkOrderDate, boolean reminderEnabled) {
        this.lastWorkOrderDate = lastWorkOrderDate;
        this.reminderEnabled = reminderEnabled;
    }

    public long getLastWorkOrderDate() {
        return lastWorkOrderDate;
    }

    public boolean isReminderEnabled() {
        return reminderEnabled;
    }
}
