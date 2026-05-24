package com.example.mainproj;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.example.mainproj.adapters.MainPagerAdapter;
import com.example.mainproj.fragments.ProfileFragment;
import com.example.mainproj.messaging.NetworkMonitor;
import com.example.mainproj.userSystem.Login_activity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends BaseActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String PREFS_NAME = "MainProjPrefs";
    private static final String KEY_LOGGED_IN_EMAIL = "logged_in_email";

    private Toolbar toolbar;
    private ViewPager viewPager;
    private BottomNavigationView bottomNav;
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private SharedPreferences prefs;
    private MainPagerAdapter pagerAdapter;
    private int lastSelectedPosition = 0;
    private boolean isInSettingsNav = false;
    private ConnectivityManager.NetworkCallback offlineDialogCallback;
    private AlertDialog offlineDialog;
    private final Handler connectivityHandler = new Handler(Looper.getMainLooper());
    private final Runnable connectivityCheckRunnable = () -> checkConnectivityAndShowDialog();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        mAuth = FirebaseAuth.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        setupViewPager();
        setupBottomNav();

        int tabPosition = getIntent().getIntExtra("tab_position", 1);
        viewPager.setCurrentItem(tabPosition, false);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main),
                (v, insets) -> {
                    Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(bars.left, bars.top, bars.right, 0);
                    return insets;
                });

        toolbar.setNavigationOnClickListener(v -> {
            androidx.drawerlayout.widget.DrawerLayout drawerLayout = findViewById(R.id.drawerLayout);
            drawerLayout.openDrawer(GravityCompat.START);
        });

        NavigationView navigationView = findViewById(R.id.navigationView);
        navigationView.setNavigationItemSelectedListener(this);

        checkConnectivityAndShowDialog();

        ConnectivityManager.NetworkCallback cb = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                runOnUiThread(() -> checkConnectivityAndShowDialog());
            }

            @Override
            public void onLost(@NonNull Network network) {
                runOnUiThread(() -> {
                    checkConnectivityAndShowDialog();
                    connectivityHandler.removeCallbacks(connectivityCheckRunnable);
                    connectivityHandler.postDelayed(connectivityCheckRunnable, 2000);
                });
            }

            @Override
            public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
                runOnUiThread(() -> checkConnectivityAndShowDialog());
            }

            @Override
            public void onUnavailable() {
                runOnUiThread(() -> checkConnectivityAndShowDialog());
            }
        };
        NetworkMonitor.getInstance(this).registerNetworkCallback(cb);
        offlineDialogCallback = cb;
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkConnectivityAndShowDialog();
    }

    @Override
    protected void onPause() {
        super.onPause();
        connectivityHandler.removeCallbacks(connectivityCheckRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (offlineDialogCallback != null) {
            NetworkMonitor.getInstance(this).unregisterNetworkCallback(offlineDialogCallback);
        }
    }

    private void checkConnectivityAndShowDialog() {
        if (!NetworkMonitor.getInstance(this).isConnected()) {
            if (offlineDialog == null || !offlineDialog.isShowing()) {
                offlineDialog = new AlertDialog.Builder(this)
                        .setTitle("No Connection")
                        .setMessage("not connected to internet, certain features unavailable.")
                        .setPositiveButton("OK", null)
                        .show();
            }
        } else {
            if (offlineDialog != null && offlineDialog.isShowing()) {
                offlineDialog.dismiss();
                offlineDialog = null;
            }
        }
    }

    private void setupViewPager() {
        viewPager = findViewById(R.id.viewPager);
        pagerAdapter = new MainPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(pagerAdapter);

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

            @Override
            public void onPageSelected(int position) {
                updateBottomNavSelection(position);
                lastSelectedPosition = position;
            }

            @Override
            public void onPageScrollStateChanged(int state) {}
        });
    }

    private void setupBottomNav() {
        bottomNav = findViewById(R.id.bottomNav);

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                viewPager.setCurrentItem(0, true);
                return true;
            } else if (itemId == R.id.nav_messages) {
                viewPager.setCurrentItem(1, true);
                return true;
            } else if (itemId == R.id.nav_profile) {
                ProfileFragment pf = getProfileFragment();
                if (pf != null) {
                    if (isInSettingsNav) {
                        pf.showProfile();
                        isInSettingsNav = false;
                    } else {
                        pf.showSettingsInstead();
                        isInSettingsNav = true;
                    }
                }
                viewPager.setCurrentItem(2, true);
                return true;
            }
            return false;
        });
    }

    private void updateBottomNavSelection(int position) {
        switch (position) {
            case 0:
                bottomNav.setSelectedItemId(R.id.nav_home);
                isInSettingsNav = false;
                break;
            case 1:
                bottomNav.setSelectedItemId(R.id.nav_messages);
                isInSettingsNav = false;
                break;
            case 2:
                bottomNav.setSelectedItemId(R.id.nav_profile);
                break;
        }
    }

    private ProfileFragment getProfileFragment() {
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.viewPager);
        if (currentFragment instanceof ProfileFragment) {
            return (ProfileFragment) currentFragment;
        }
        return null;
    }

    @Override
    public void onBackPressed() {
        if (viewPager.getCurrentItem() == 2 && isInSettingsNav) {
            ProfileFragment pf = getProfileFragment();
            if (pf != null) {
                pf.showProfile();
                isInSettingsNav = false;
            }
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull android.view.MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_logout) {
            mAuth.signOut();
            mGoogleSignInClient.signOut();
            prefs.edit().remove(KEY_LOGGED_IN_EMAIL).apply();
            startActivity(new Intent(this, Login_activity.class));
            finish();
        }

        androidx.drawerlayout.widget.DrawerLayout drawerLayout = findViewById(R.id.drawerLayout);
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }
}