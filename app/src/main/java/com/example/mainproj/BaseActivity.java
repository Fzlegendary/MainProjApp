package com.example.mainproj;

import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public abstract class BaseActivity extends AppCompatActivity {

    protected BottomNavigationView bottomNav;
    private int activeNavItemId = -1;

    protected void setActiveNav(int menuItemId) {
        activeNavItemId = menuItemId;
        bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(menuItemId);
            setupBottomNavListeners();
        }
    }

    private void setupBottomNavListeners() {
        if (bottomNav == null) return;
        
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == activeNavItemId) {
                return true;
            }
            
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
}