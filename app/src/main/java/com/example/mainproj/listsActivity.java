package com.example.mainproj;

import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.mainproj.whitelistPlaceholder.FragmentWhitelist;

public class listsActivity extends AppCompatActivity {

    private Button btnWhitelist, btnBlacklist;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_lists);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Buttons to switch fragments
        btnWhitelist = findViewById(R.id.btnWhitelist);
        btnBlacklist = findViewById(R.id.btnBlacklist);

        // Default fragment
        loadFragment(new FragmentWhitelist());

        btnWhitelist.setOnClickListener(v -> loadFragment(new FragmentWhitelist()));
        btnBlacklist.setOnClickListener(v -> loadFragment(new com.example.mainproj.BlacklistPlaceholder.FragmentBlacklist()));
    }

    private void loadFragment(Fragment fragment) {
        FragmentManager fm = getSupportFragmentManager();
        fm.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }
}
