package com.example.mainproj;

import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public abstract class BaseActivity extends AppCompatActivity {

    protected BottomNavigationView bottomNav;

    @Override
    protected void onResume() {
        super.onResume();
    }

    protected void setActiveNav(int menuItemId) {
    }

    protected void clearActiveNav() {
    }
}