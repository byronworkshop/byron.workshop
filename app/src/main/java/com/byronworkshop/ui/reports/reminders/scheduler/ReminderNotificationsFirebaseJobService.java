package com.byronworkshop.ui.reports.reminders.scheduler;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.core.content.ContextCompat;

import com.byronworkshop.R;
import com.byronworkshop.ui.mainactivity.adapter.pojo.Motorcycle;
import com.byronworkshop.ui.reports.reminders.RemindersActivity;
import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;
import com.google.android.gms.tasks.OnCanceledListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Calendar;

public class ReminderNotificationsFirebaseJobService extends JobService {

    private static final String REMINDER_NOTIFICATIONS_CHANNEL_ID = "reminder_notifications_channel";

    private boolean canContinue;

    @Override
    public boolean onStartJob(@NonNull JobParameters  job) {
        this.canContinue = true;
        notifyOutdatedWos(this, job);
        return true;
    }

    @Override
    public boolean onStopJob(@NonNull JobParameters job) {
        this.canContinue = false;
        return true;
    }

    // ---------------------------------------------------------------------------------------------
    // custom methods
    // ---------------------------------------------------------------------------------------------
    private void notifyOutdatedWos(Context context, JobParameters job) {
        if (!canContinue) {
            jobFinished(job, false);
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            fetchMaxDaysFromPreferences(context, user, job);
        }
    }

    private void fetchMaxDaysFromPreferences(Context context, FirebaseUser user, JobParameters job) {
        if (!canContinue) {
            jobFinished(job, false);
            return;
        }

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        int maxDays = Integer.parseInt(preferences.getString(getString(R.string.pref_max_elapsed_time_last_service_key), getString(R.string.pref_max_elapsed_time_last_service_default_value)));

        // trigger next step, query outdated work orders
        getOutdatedWorkOrders(context, user, maxDays, job);
    }

    private void getOutdatedWorkOrders(
            final Context context, final FirebaseUser user, final int maxDays, final JobParameters job) {
        if (!canContinue) {
            jobFinished(job, false);
            return;
        }

        Calendar lowRange = Calendar.getInstance();
        lowRange.add(Calendar.DATE, maxDays * -1);
        lowRange.set(Calendar.HOUR_OF_DAY, 0);
        lowRange.set(Calendar.MINUTE, 0);
        lowRange.set(Calendar.SECOND, 0);
        lowRange.set(Calendar.MILLISECOND, 0);

        Calendar highRange = Calendar.getInstance();
        highRange.add(Calendar.DATE, maxDays * -1);
        highRange.set(Calendar.HOUR_OF_DAY, 23);
        highRange.set(Calendar.MINUTE, 59);
        highRange.set(Calendar.SECOND, 59);
        highRange.set(Calendar.MILLISECOND, 0);

        // paths
        FieldPath reminderEnabledPath = FieldPath.of("metadata", "reminderEnabled");
        FieldPath lastWorkOrderEndDatePath = FieldPath.of("metadata", "lastWorkOrderEndDate");

        // construct query
        final CollectionReference mMotorcyclesCollReference =
                FirebaseFirestore.getInstance().collection("users")
                        .document(user.getUid()).collection("motorcycles");

        Query query = mMotorcyclesCollReference
                .whereEqualTo(reminderEnabledPath, true)
                .whereGreaterThanOrEqualTo(lastWorkOrderEndDatePath, lowRange.getTime())
                .whereLessThanOrEqualTo(lastWorkOrderEndDatePath, highRange.getTime())
                .orderBy(lastWorkOrderEndDatePath, Query.Direction.DESCENDING);

        // set listener
        query.get()
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        jobFinished(job, false);
                    }
                })
                .addOnCanceledListener(new OnCanceledListener() {
                    @Override
                    public void onCanceled() {
                        jobFinished(job, false);
                    }
                })
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        if (queryDocumentSnapshots == null) {
                            jobFinished(job, false);
                            return;
                        }

                        if (!canContinue) {
                            jobFinished(job, false);
                            return;
                        }

                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            if (doc.exists()) {
                                Motorcycle motorcycle = doc.toObject(Motorcycle.class);
                                // show notification
                                showNotification(context, user, motorcycle.getBrand(), motorcycle.getLicensePlateNumber(), maxDays);
                            }
                        }

                        jobFinished(job, false);
                    }
                });
    }

    private void showNotification(Context context, FirebaseUser user, String brand, String licensePlateNumber, int maxDays) {
        NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel mChannel = new NotificationChannel(
                    REMINDER_NOTIFICATIONS_CHANNEL_ID,
                    context.getString(R.string.remainder_notifications_channel_name),
                    NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(mChannel);
        }


        // remainder pending intent
        Intent remindersActivity = new Intent(context, RemindersActivity.class);
        Bundle reminderBundle = new Bundle();
        reminderBundle.putString(RemindersActivity.KEY_UID, user.getUid());
        remindersActivity.putExtras(reminderBundle);

        TaskStackBuilder taskStackBuilder = TaskStackBuilder.create(context);
        taskStackBuilder.addNextIntentWithParentStack(remindersActivity);
        PendingIntent resultPendingIntent = taskStackBuilder
                .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        // build notification
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, REMINDER_NOTIFICATIONS_CHANNEL_ID)
                .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
                .setSmallIcon(smallIcon())
                .setLargeIcon(largeIcon(context))
                .setContentTitle(context.getString(R.string.remainder_notifications_title))
                .setContentText(context.getString(R.string.remainder_notifications_msg, brand, licensePlateNumber))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(
                        context.getString(R.string.remainder_notifications_big_msg, maxDays, brand, licensePlateNumber)))
                .setDefaults(Notification.DEFAULT_VIBRATE | Notification.DEFAULT_SOUND)
                .setContentIntent(resultPendingIntent)
                .setAutoCancel(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN
                && Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            notificationBuilder.setPriority(NotificationCompat.PRIORITY_HIGH);
        }

        notificationManager.notify((int) System.currentTimeMillis(), notificationBuilder.build());
    }

    private static int smallIcon() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return R.drawable.ic_notification_motorcycle_white_vector;
        } else {
            return R.drawable.ic_notification_motorcycle_white_png;
        }
    }

    private static Bitmap largeIcon(Context context) {
        Resources res = context.getResources();
        return BitmapFactory.decodeResource(res, R.drawable.ic_notification_motorcycle_black_vector);
    }
}
