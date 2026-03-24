package com.example.mainproj;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.mainproj.lists.listsActivity;
import com.example.mainproj.messaging.MessagingActivity;
import com.example.mainproj.userSystem.Login_activity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends BaseActivity implements NavigationView.OnNavigationItemSelectedListener {

    private Toolbar toolbar;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        setActiveNav(R.id.nav_home);

        mAuth = FirebaseAuth.getInstance();

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main),
                (v, insets) -> {
                    Insets bars = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars());
                    v.setPadding(
                            bars.left,
                            bars.top,
                            bars.right,
                            0);
                    return insets;
                });

        toolbar.setNavigationOnClickListener(v -> {
            androidx.drawerlayout.widget.DrawerLayout drawerLayout = findViewById(R.id.drawerLayout);
            drawerLayout.openDrawer(GravityCompat.START);
        });

        NavigationView navigationView = findViewById(R.id.navigationView);
        navigationView.setNavigationItemSelectedListener(this);

        MaterialButton btnMessaging = findViewById(R.id.btnMessaging);
        MaterialButton btnGateControl = findViewById(R.id.btnGateControl);
        MaterialButton btnServerSettings = findViewById(R.id.btnServerSettings);
        MaterialButton btnAccountManagement = findViewById(R.id.btnAccountManagement);
        MaterialButton btnBlacklist = findViewById(R.id.btnBlacklist);

        btnMessaging.setOnClickListener(v ->
                startActivity(new Intent(this, MessagingActivity.class)));

        btnGateControl.setOnClickListener(v ->
                startActivity(new Intent(this, gatesMain.class)));

        btnServerSettings.setOnClickListener(v ->
                startActivity(new Intent(this, ServerSettingsActivity.class)));

        btnAccountManagement.setOnClickListener(v ->
                startActivity(new Intent(this, AccountManagementActivity.class)));

        btnBlacklist.setOnClickListener(v ->
                startActivity(new Intent(this, listsActivity.class)));
    }

    @Override
    public boolean onNavigationItemSelected(android.view.MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_logout) {
            mAuth.signOut();
            startActivity(new Intent(this, Login_activity.class));
            finish();
        }

        androidx.drawerlayout.widget.DrawerLayout drawerLayout = findViewById(R.id.drawerLayout);
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }
}
