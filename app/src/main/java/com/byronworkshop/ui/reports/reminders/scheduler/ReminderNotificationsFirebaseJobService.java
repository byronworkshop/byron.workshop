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
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.ContextCompat;

import com.byronworkshop.BuildConfig;
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
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import static com.byronworkshop.ui.reports.reminders.RemindersActivity.DEFAULT_MAX_WO_ELAPSED_DAYS;
import static com.byronworkshop.ui.reports.reminders.RemindersActivity.MAX_LAST_WO_ELAPSED_DAYS_KEY;

public class ReminderNotificationsFirebaseJobService extends JobService {

    private static final String REMINDER_NOTIFICATIONS_CHANNEL_ID = "reminder_notifications_channel";

    private boolean canContinue;

    @Override
    public boolean onStartJob(JobParameters job) {
        this.canContinue = true;
        notifyOutdatedWos(this, job);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters job) {
        this.canContinue = false;
        return true;
    }

    // ---------------------------------------------------------------------------------------------
    // custom methods
    // ---------------------------------------------------------------------------------------------
    private void notifyOutdatedWos(Context context, JobParameters job) {
        if (!canContinue) {
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            fetchMaxDaysFromPreferences(context, user, job);
        }
    }

    private void fetchMaxDaysFromPreferences(Context context, FirebaseUser user, JobParameters job) {
        if (!canContinue) {
            return;
        }

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        int maxDays = preferences.getInt(MAX_LAST_WO_ELAPSED_DAYS_KEY, DEFAULT_MAX_WO_ELAPSED_DAYS);

        // trigger next step, query outdated work orders
        getOutdatedWorkOrders(context, user, maxDays, job);

        // async check remote config for parameter updates
        fetchMaxDaysFromRemoteConfig(maxDays);
    }

    private void fetchMaxDaysFromRemoteConfig(final int currMaxDays) {
        if (!canContinue) {
            return;
        }

        // config FirebaseRemoteConfig
        final FirebaseRemoteConfig mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(BuildConfig.DEBUG)
                .build();
        mFirebaseRemoteConfig.setConfigSettings(configSettings);

        Map<String, Object> defaultConfigMap = new HashMap<>();
        defaultConfigMap.put(MAX_LAST_WO_ELAPSED_DAYS_KEY, DEFAULT_MAX_WO_ELAPSED_DAYS);
        mFirebaseRemoteConfig.setDefaults(defaultConfigMap);

        long cacheExpiration = 3600; // 1 hour in seconds

        // If developer mode is enabled reduce cacheExpiration to 0 so that each fetch goes to the
        // server. This should not be used in release builds.
        if (mFirebaseRemoteConfig.getInfo().getConfigSettings().isDeveloperModeEnabled()) {
            cacheExpiration = 0;
        }

        mFirebaseRemoteConfig.fetch(cacheExpiration)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // get days
                        mFirebaseRemoteConfig.activateFetched();
                        int remoteConfigMaxDays = (int) mFirebaseRemoteConfig.getLong(MAX_LAST_WO_ELAPSED_DAYS_KEY);

                        if (remoteConfigMaxDays != currMaxDays) {
                            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                            preferences.edit().putInt(MAX_LAST_WO_ELAPSED_DAYS_KEY, remoteConfigMaxDays).apply();
                        }
                    }
                });
    }

    private void getOutdatedWorkOrders(
            final Context context, final FirebaseUser user, final int maxDays, final JobParameters job) {
        if (!canContinue) {
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
        FieldPath lastWorkOrderDatePath = FieldPath.of("metadata", "lastWorkOrderDate");

        // construct query
        final CollectionReference mMotorcyclesCollReference =
                FirebaseFirestore.getInstance().collection("users")
                        .document(user.getUid()).collection("motorcycles");

        Query query = mMotorcyclesCollReference
                .whereEqualTo(reminderEnabledPath, true)
                .whereGreaterThan(lastWorkOrderDatePath, -1)
                .whereGreaterThanOrEqualTo(lastWorkOrderDatePath, lowRange.getTimeInMillis())
                .whereLessThanOrEqualTo(lastWorkOrderDatePath, highRange.getTimeInMillis())
                .orderBy(lastWorkOrderDatePath, Query.Direction.DESCENDING);

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
                            return;
                        }

                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            if (doc.exists()) {
                                final Motorcycle motorcycle = doc.toObject(Motorcycle.class);
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
