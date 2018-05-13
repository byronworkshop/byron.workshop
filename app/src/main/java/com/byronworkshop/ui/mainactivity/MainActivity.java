package com.byronworkshop.ui.mainactivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
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
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.byronworkshop.R;
import com.byronworkshop.shared.dialogs.EditMotorcycleDialogFragment;
import com.byronworkshop.ui.detailsactivity.DetailsActivity;
import com.byronworkshop.ui.mainactivity.adapter.MotorcycleRVAdapter;
import com.byronworkshop.ui.mainactivity.adapter.pojo.Motorcycle;
import com.byronworkshop.ui.mainactivity.pojo.ByronUser;
import com.byronworkshop.ui.reports.SettingsActivity;
import com.byronworkshop.ui.reports.income.IncomeActivity;
import com.byronworkshop.ui.reports.reminders.RemindersActivity;
import com.byronworkshop.ui.reports.reminders.scheduler.ReminderNotificationsUtilities;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, MotorcycleRVAdapter.ListItemClickListener {

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
    private ActionBarDrawerToggle mDrawerToggle;
    private RecyclerView mMotorcycleRecyclerView;
    private View emptyView;

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

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // firebase initialization
        this.mFirebaseAuth = FirebaseAuth.getInstance();
        this.mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        // ui
        this.mMotorcycleRecyclerView = findViewById(R.id.content_main_rv_motorcycles);
        this.mMotorcycleRecyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        this.mMotorcycleRecyclerView.setItemAnimator(new DefaultItemAnimator());
        this.emptyView = findViewById(R.id.content_main_rv_empty_view);

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
    protected void onStart() {
        super.onStart();

        this.mFirebaseAuth.addAuthStateListener(this.mAuthStateListener);
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (this.mAuthStateListener != null) {
            this.mFirebaseAuth.removeAuthStateListener(this.mAuthStateListener);
        }

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
                this.showSignOutConfimDialog();
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
                this.showSignOutConfimDialog();
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
        this.bUser = new ByronUser(user.getUid(), user.getDisplayName(), user.getEmail(), user.getPhotoUrl());

        logSignInEvent();
        loadMotorcycleCollReference();
        loadeUserNavHeader(this.bUser);
        attachMotorcycleRVAdapter();
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

    private void loadeUserNavHeader(ByronUser bUser) {
        NavigationView navView = findViewById(R.id.nav_view);

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
        NavigationView navView = findViewById(R.id.nav_view);

        ((TextView) navView.getHeaderView(0).findViewById(R.id.account_name)).setText("");
        ((TextView) navView.getHeaderView(0).findViewById(R.id.account_email)).setText("");
        Glide.with(this)
                .load(R.drawable.ic_avatar_placeholder)
                .apply(RequestOptions.circleCropTransform())
                .into(((ImageView) navView.getHeaderView(0).findViewById(R.id.account_avatar)));
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

    private void showSignOutConfimDialog() {
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
}
