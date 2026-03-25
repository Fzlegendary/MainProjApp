package com.example.mainproj;

import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public abstract class BaseActivity extends AppCompatActivity {

    protected BottomNavigationView bottomNav;
    private int activeNavItemId = -1;
    private boolean bottomNavListenerSetup = false;

    @Override
    protected void onResume() {
        super.onResume();
        setupBottomNavOnce();
    }

    private void setupBottomNavOnce() {
        if (bottomNavListenerSetup) return;
        
        bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav == null) return;
        
        bottomNavListenerSetup = true;
        
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            
            if (itemId == R.id.nav_home) {
                if (!(this instanceof MainActivity)) {
                    startActivity(new android.content.Intent(this, MainActivity.class));
                    finish();
                }
            } else if (itemId == R.id.nav_profile) {
                if (!(this instanceof ProfileDisplayerActivity)) {
                    startActivity(new android.content.Intent(this, ProfileDisplayerActivity.class));
                    finish();
                }
            } else if (itemId == R.id.nav_settings) {
                if (!(this instanceof SettingsActivity)) {
                    startActivity(new android.content.Intent(this, SettingsActivity.class));
                    finish();
                }
            }
            return true;
        });
    }

    protected void setActiveNav(int menuItemId) {
        activeNavItemId = menuItemId;
        setupBottomNavOnce();
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(menuItemId);
        }
    }

    protected void clearActiveNav() {
        setupBottomNavOnce();
        if (bottomNav != null) {
            for (int i = 0; i < bottomNav.getMenu().size(); i++) {
                bottomNav.getMenu().getItem(i).setChecked(false);
            }
        }
    }
}