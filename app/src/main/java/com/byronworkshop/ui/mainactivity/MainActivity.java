package com.byronworkshop.ui.mainactivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.byronworkshop.R;
import com.byronworkshop.shared.dialogs.EditMotorcycleDialogFragment;
import com.byronworkshop.ui.detailsactivity.DetailsActivity;
import com.byronworkshop.ui.mainactivity.adapter.MotorcycleRVAdapter;
import com.byronworkshop.ui.mainactivity.adapter.pojo.Motorcycle;
import com.byronworkshop.ui.mainactivity.pojo.ByronUser;
import com.byronworkshop.ui.reports.income.IncomeActivity;
import com.byronworkshop.ui.reports.reminders.RemindersActivity;
import com.byronworkshop.ui.reports.reminders.scheduler.ReminderNotificationsUtilities;
import com.byronworkshop.ui.settings.SettingsActivity;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        MotorcycleRVAdapter.ListItemClickListener,
        EditMotorcycleDialogFragment.MotorcycleDialogCallback {

    // statics
    private static final int RC_SIGN_IN = 101;
    private static final String KEY_SEARCH = "key_search";

    // firebase analytics events
    private static final String EVENT_SIGN_IN = "show_motorcycle_list";
    private static final String EVENT_SIGN_OUT = "sign_out";

    // vars
    private ByronUser bUser;
    private String searchQuery;

    // ui
    private CoordinatorLayout mMainContainer;
    private ActionBarDrawerToggle mDrawerToggle;
    private RecyclerView mMotorcycleRecyclerView;
    private View emptyView;
    private ImageView ivHeaderBg;
    private NavigationView navView;

    // firebase ui
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAnalytics mFirebaseAnalytics;
    private CollectionReference mMotorcyclesCollReference;
    private FirebaseAuth.AuthStateListener mAuthStateListener;

    // mMotorcycleAdapter
    private FirestoreRecyclerAdapter mMotorcycleAdapter;

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(KEY_SEARCH, this.searchQuery);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState != null && savedInstanceState.containsKey(KEY_SEARCH)) {
            this.searchQuery = savedInstanceState.getString(KEY_SEARCH);
        }

        // navigation drawer configuration
        Toolbar toolbar = findViewById(R.id.activity_main_toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        this.mDrawerToggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(this.mDrawerToggle);

        this.navView = findViewById(R.id.nav_view);
        this.navView.setNavigationItemSelectedListener(this);

        // firebase initialization
        this.mFirebaseAuth = FirebaseAuth.getInstance();
        this.mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        // ui
        this.mMainContainer = findViewById(R.id.activity_main_container);
        this.mMotorcycleRecyclerView = findViewById(R.id.content_main_rv_motorcycles);
        this.mMotorcycleRecyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        this.mMotorcycleRecyclerView.setItemAnimator(new DefaultItemAnimator());
        this.emptyView = findViewById(R.id.content_main_rv_empty_view);
        this.ivHeaderBg = navView.getHeaderView(0).findViewById(R.id.account_bg);

        // set image header bg
        ColorDrawable imagePlaceholder = new ColorDrawable(ContextCompat.getColor(this, R.color.colorPlaceholder));
        RequestOptions options = RequestOptions.placeholderOf(imagePlaceholder);

        Glide.with(this)
                .load(R.drawable.header_bg)
                .apply(options)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(ivHeaderBg);

        // authentication
        this.mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    onSignedInInitialize(user);
                } else {
                    onSignOutCleanup();
                    startActivityForResult(
                            AuthUI.getInstance()
                                    .createSignInIntentBuilder()
                                    .setIsSmartLockEnabled(false)
                                    .setLogo(R.drawable.ic_brand_shield_protection)
                                    .setAvailableProviders(
                                            Arrays.asList(
                                                    new AuthUI.IdpConfig.EmailBuilder().build(),
                                                    new AuthUI.IdpConfig.GoogleBuilder().build())
                                    ).build(),
                            RC_SIGN_IN);
                }
            }
        };

        // adding a brand new motorcycle
        FloatingActionButton fab = findViewById(R.id.activity_main_fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditMotorcycleDialogFragment.showEditMotorcycleDialog(
                        MainActivity.this,
                        getSupportFragmentManager(),
                        bUser.getUid(),
                        null,
                        null);
            }
        });

        // initialize notifications scheduler
        ReminderNotificationsUtilities.scheduleReminderNotifications(this);
    }

    // ---------------------------------------------------------------------------------------------
    // activity lifecycle
    // ---------------------------------------------------------------------------------------------
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_CANCELED) {
                // get out of here
                finish();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // this should be called here, otherwise onActivityResult won't be called properly
        this.mFirebaseAuth.addAuthStateListener(this.mAuthStateListener);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (this.mAuthStateListener != null) {
            this.mFirebaseAuth.removeAuthStateListener(this.mAuthStateListener);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        this.detachMotorcycleRVAdapter();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        this.mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        this.mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        MenuItem search = menu.findItem(R.id.menu_main_search);
        SearchView searchView = (SearchView) search.getActionView();
        searchView.setMaxWidth(Integer.MAX_VALUE);
        searchView.setImeOptions(EditorInfo.IME_ACTION_DONE);

        if (!TextUtils.isEmpty(this.searchQuery)) {
            search.expandActionView();
            searchView.setQuery(this.searchQuery, false);
        } else {
            searchView.clearFocus();
            searchView.onActionViewCollapsed();
            searchView.setQuery(null, false);
        }

        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                searchQuery = null;
                attachMotorcycleRVAdapter();
                return false;
            }
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                searchQuery = newText;
                attachMotorcycleRVAdapter();
                return true;
            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_main_action_settings:
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                startActivity(settingsIntent);
                return true;
            case R.id.menu_main_action_sign_out:
                this.showSignOutConfirmDialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        switch (id) {
            case R.id.nav_add_bike:
                EditMotorcycleDialogFragment.showEditMotorcycleDialog(
                        this,
                        getSupportFragmentManager(),
                        bUser.getUid(),
                        null,
                        null);
                break;
            case R.id.nav_income:
                Intent incomeIntent = new Intent(this, IncomeActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString(IncomeActivity.KEY_UID, this.bUser.getUid());
                incomeIntent.putExtras(bundle);

                startActivity(incomeIntent);
                break;
            case R.id.nav_reminders:
                Intent remindersIntent = new Intent(this, RemindersActivity.class);
                Bundle reminderBundle = new Bundle();
                reminderBundle.putString(RemindersActivity.KEY_UID, this.bUser.getUid());
                remindersIntent.putExtras(reminderBundle);

                startActivity(remindersIntent);
                break;
            case R.id.nav_exit:
                this.showSignOutConfirmDialog();
                break;
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);

        return true;
    }

    // ---------------------------------------------------------------------------------------------
    // custom methods
    // ---------------------------------------------------------------------------------------------
    private void onSignedInInitialize(FirebaseUser user) {
        // check providers list
        if (user.getProviders() == null) {
            AuthUI.getInstance().signOut(MainActivity.this);
            return;
        }

        // check if provider is 'password'
        String providerId = user.getProviders().get(0);
        if (providerId.equals(EmailAuthProvider.PROVIDER_ID)) {
            if (!user.isEmailVerified()) {
                showConfirmEmailDialog(user);
                return;
            }
        }

        // if not 'password' or already verified then sign in normally
        this.bUser = new ByronUser(user.getUid(), user.getDisplayName(), user.getEmail(), user.getPhotoUrl());

        logSignInEvent();
        loadMotorcycleCollReference();
        loadUserNavHeader(this.bUser);
        attachMotorcycleRVAdapter();
    }

    private void showConfirmEmailDialog(final FirebaseUser user) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.firebase_auth_alert_email_verification_title)
                .setCancelable(false)
                .setMessage(R.string.firebase_auth_alert_email_verification_msg)
                .setPositiveButton(R.string.firebase_auth_alert_email_verification_positive, null)
                .setNegativeButton(R.string.firebase_auth_alert_email_verification_negative, null);

        AlertDialog alertDialog = builder.create();
        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(final DialogInterface dialog) {
                Button buttonPositive = ((android.support.v7.app.AlertDialog) dialog).getButton(DialogInterface.BUTTON_POSITIVE);
                final Button buttonNegative = ((android.support.v7.app.AlertDialog) dialog).getButton(DialogInterface.BUTTON_NEGATIVE);

                buttonPositive.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        buttonNegative.setEnabled(false);
                        // send email verification
                        user.sendEmailVerification()
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        buttonNegative.setEnabled(true);

                                        if (task.isSuccessful()) {
                                            // show success msg
                                            Toast.makeText(MainActivity.this, getString(R.string.firebase_auth_alert_email_verification_success, user.getEmail()), Toast.LENGTH_LONG).show();

                                            // sign out
                                            AuthUI.getInstance().signOut(MainActivity.this);
                                            dialog.dismiss();
                                        } else {
                                            // show failure msg
                                            Toast.makeText(MainActivity.this, R.string.firebase_auth_alert_email_verification_failure, Toast.LENGTH_LONG).show();
                                        }
                                    }
                                });
                    }
                });

                buttonNegative.setFocusable(false);
                buttonNegative.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        AuthUI.getInstance().signOut(MainActivity.this);
                        dialog.dismiss();
                    }
                });
            }
        });

        alertDialog.show();
    }

    private void onSignOutCleanup() {
        logSignOutEvent();
        unloadUserNavHeader();
        detachMotorcycleRVAdapter();
    }

    private void logSignInEvent() {
        this.mFirebaseAnalytics.logEvent(EVENT_SIGN_IN, null);
    }

    private void logSignOutEvent() {
        this.mFirebaseAnalytics.logEvent(EVENT_SIGN_OUT, null);
    }

    private void loadMotorcycleCollReference() {
        this.mMotorcyclesCollReference = FirebaseFirestore.getInstance().collection("users").document(this.bUser.getUid()).collection("motorcycles");
    }

    private void loadUserNavHeader(ByronUser bUser) {
        ((TextView) navView.getHeaderView(0).findViewById(R.id.account_name)).setText(bUser.getName());
        ((TextView) navView.getHeaderView(0).findViewById(R.id.account_email)).setText(bUser.getEmail());

        if (bUser.getPhotoUrl() != null) {
            ImageView avatar = navView.getHeaderView(0).findViewById(R.id.account_avatar);
            Glide.with(this)
                    .load(bUser.getPhotoUrl())
                    .apply(RequestOptions.circleCropTransform())
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(avatar);
        } else {
            ((ImageView) navView.getHeaderView(0).findViewById(R.id.account_avatar))
                    .setImageResource(R.drawable.ic_avatar_placeholder);
        }
    }

    private void unloadUserNavHeader() {
        ((TextView) navView.getHeaderView(0).findViewById(R.id.account_name)).setText("");
        ((TextView) navView.getHeaderView(0).findViewById(R.id.account_email)).setText("");
        Glide.with(this)
                .load(R.drawable.ic_avatar_placeholder)
                .apply(RequestOptions.circleCropTransform())
                .into(((ImageView) navView.getHeaderView(0).findViewById(R.id.account_avatar)));
        this.bUser = null;
    }

    private void attachMotorcycleRVAdapter() {
        // prepare query
        String filter = !TextUtils.isEmpty(this.searchQuery) ? this.searchQuery.trim().toUpperCase() : "";
        Query query = this.mMotorcyclesCollReference.orderBy("brand").startAt(filter).endAt(filter + "\\uf8ff");

        // prepare recycler options
        FirestoreRecyclerOptions<Motorcycle> options = new FirestoreRecyclerOptions.Builder<Motorcycle>()
                .setQuery(query, Motorcycle.class)
                .build();

        // manually stop previous existent adapter
        if (this.mMotorcycleAdapter != null) {
            this.mMotorcycleAdapter.stopListening();
            this.mMotorcycleAdapter = null;
        }

        // create new adapter and start listening
        this.mMotorcycleAdapter = new MotorcycleRVAdapter(this, options, this, this.emptyView);
        this.mMotorcycleAdapter.startListening();
        this.mMotorcycleRecyclerView.setAdapter(this.mMotorcycleAdapter);
    }

    private void detachMotorcycleRVAdapter() {
        // to clean the list and stop the adapter
        if (this.mMotorcycleAdapter != null) {
            this.mMotorcycleAdapter.stopListening();
            this.mMotorcycleAdapter = null;
            this.mMotorcycleRecyclerView.setAdapter(null);
        }
    }

    private void showSignOutConfirmDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.sign_out_title))
                .setMessage(getString(R.string.sign_out_message))
                .setPositiveButton(getString(R.string.sign_out_ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        AuthUI.getInstance().signOut(MainActivity.this);
                    }
                })
                .setNegativeButton(getString(R.string.sign_out_cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        builder.show();
    }

    // ---------------------------------------------------------------------------------------------
    // RV Adapter listener
    // ---------------------------------------------------------------------------------------------
    @Override
    public void onListItemClick(String motorcycleId, Motorcycle motorcycle) {
        Bundle bundle = new Bundle();
        bundle.putString(DetailsActivity.KEY_MOTORCYCLE_ID, motorcycleId);
        bundle.putString(DetailsActivity.KEY_UID, this.bUser.getUid());

        Intent detailsIntent = new Intent(this, DetailsActivity.class);
        detailsIntent.putExtras(bundle);

        startActivity(detailsIntent);

    }

    // ---------------------------------------------------------------------------------------------
    // Edit Motorcycle callbacks
    // ---------------------------------------------------------------------------------------------
    @Override
    public void onMotorcycleSaveError() {
        Snackbar.make(this.mMainContainer, getString(R.string.dialog_edit_motorcycle_error_cannot_save), Snackbar.LENGTH_LONG).show();
    }

    @Override
    public void onMotorcycleSaved(@NonNull String msg, @Nullable final String motorcycleId) {
        boolean actionEnabled = motorcycleId != null;

        Snackbar snackbar = Snackbar.make(this.mMainContainer, msg, actionEnabled ? Snackbar.LENGTH_INDEFINITE : Snackbar.LENGTH_LONG);
        if (actionEnabled) {
            snackbar.setAction(getString(R.string.dialog_edit_motorcycle_edition_action_view_lbl), new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(MainActivity.this, DetailsActivity.class);

                    Bundle b = new Bundle();
                    b.putString(DetailsActivity.KEY_UID, bUser.getUid());
                    b.putString(DetailsActivity.KEY_MOTORCYCLE_ID, motorcycleId);

                    intent.putExtras(b);

                    startActivity(intent);
                }
            });
        }
        snackbar.show();
    }
}
