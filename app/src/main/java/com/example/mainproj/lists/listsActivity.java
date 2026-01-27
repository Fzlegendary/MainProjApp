package com.example.mainproj.lists;

import android.graphics.ColorSpace;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.ColorInt;
import androidx.appcompat.app.AppCompatActivity;
import androidx.compose.ui.graphics.colorspace.Rgb;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.example.mainproj.ListsPagerAdapter;
import com.example.mainproj.R;

public class listsActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private View indicatorSlider, indicatorBar;
    private LinearLayout indicatorButtons;
    private TextView fragmentTitle;

    // Example titles for fragments (adjust to your adapter)
    private String[] pageTitles = {"Whitelist", "Blacklist"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_lists);

        // Edge-to-edge padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        viewPager = findViewById(R.id.viewPager);
        indicatorSlider = findViewById(R.id.indicatorSlider);
        indicatorBar = findViewById(R.id.indicatorBar);
        indicatorButtons = findViewById(R.id.indicatorButtons);
        fragmentTitle = findViewById(R.id.fragmentTitle);

        // Set up ViewPager adapter
        ListsPagerAdapter adapter = new ListsPagerAdapter(this);
        viewPager.setAdapter(adapter);

        int textColor = 0xFF6200EE;

        // Create buttons dynamically for each page
        int pageCount = adapter.getItemCount();
        for (int i = 0; i < pageCount; i++) {
            Button btn = new Button(this);
            btn.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f));
            btn.setBackgroundColor(0x00000000); // transparent
            final int pageIndex = i;
            btn.setTextColor(textColor);
            btn.setText(pageTitles[i]);
            btn.setOnClickListener(v -> viewPager.setCurrentItem(pageIndex, true));
            indicatorButtons.addView(btn);
        }

        // Set initial title
        if (pageTitles.length > 0) fragmentTitle.setText(pageTitles[0]);

        // Update slider on scroll
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels);

                if (pageCount == 0) return;
                int barWidth = indicatorBar.getWidth();
                int sliderWidth = barWidth / pageCount;

                indicatorSlider.getLayoutParams().width = sliderWidth;
                indicatorSlider.setTranslationX((position + positionOffset) * sliderWidth);
                indicatorSlider.requestLayout();
            }

            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                if (position < pageTitles.length) {
                    fragmentTitle.setText(pageTitles[position]);
                }
            }
        });
    }
}
