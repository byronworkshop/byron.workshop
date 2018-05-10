package com.byronworkshop.ui.reports.reminders.scheduler;

import android.content.Context;

import com.firebase.jobdispatcher.Driver;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.Lifetime;
import com.firebase.jobdispatcher.Trigger;

import java.util.concurrent.TimeUnit;

public class ReminderNotificationsUtilities {

    private static final String REMINDER_NOTIF_JOB_TAG = "reminder_notif_tag";

    private static final int REMINDER_NOTIF_INTERVAL_DAYS = 1;
    private static final int REMINDER_FLEXY_INTERVAL_HOURS = 1;
    private static final int REMINDER_NOTIF_INTERVAL_SECONDS = (int) (TimeUnit.DAYS.toSeconds(REMINDER_NOTIF_INTERVAL_DAYS));
    private static final int SYNC_FLEXTIME_SECONDS = (int) (TimeUnit.HOURS.toSeconds(REMINDER_FLEXY_INTERVAL_HOURS));

    private static boolean sInitialized;

    public static synchronized void scheduleReminderNotifications(Context context) {
        if (sInitialized) {
            return;
        }

        Driver driver = new GooglePlayDriver(context);
        FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(driver);

        Job constraintReminderNotifJob = dispatcher.newJobBuilder()
                /* The Service that will be used to write to preferences */
                .setService(ReminderNotificationsFirebaseJobService.class)
                /*
                 * Set the UNIQUE tag used to identify this Job.
                 */
                .setTag(REMINDER_NOTIF_JOB_TAG)
                /*
                 * setLifetime sets how long this job should persist. The options are to keep the
                 * Job "forever" or to have it die the next time the device boots up.
                 */
                .setLifetime(Lifetime.FOREVER)
                /*
                 * We want these reminders to continuously happen, so we tell this Job to recur.
                 */
                .setRecurring(true)
                /*
                 * We want the reminders to happen every day or so. The first argument for
                 * Trigger class's static executionWindow method is the start of the time frame
                 * when the
                 * job should be performed. The second argument is the latest point in time at
                 * which the data should be synced. Please note that this end time is not
                 * guaranteed, but is more of a guideline for FirebaseJobDispatcher to go off of.
                 */
                .setTrigger(Trigger.executionWindow(
                        REMINDER_NOTIF_INTERVAL_SECONDS,
                        REMINDER_NOTIF_INTERVAL_SECONDS + SYNC_FLEXTIME_SECONDS))
                /*
                 * If a Job with the tag with provided already exists, this new job will replace
                 * the old one.
                 */
                .setReplaceCurrent(true)
                /* Once the Job is ready, call the builder's build method to return the Job */
                .build();

        /* Schedule the Job with the dispatcher */
        dispatcher.schedule(constraintReminderNotifJob);

        /* The job has been initialized */
        sInitialized = true;
    }
}
