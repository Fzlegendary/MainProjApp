package com.example.mainproj.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.viewpager.widget.ViewPager;

public class MainNavViewPager extends ViewPager {

    private static final float SWIPE_THRESHOLD = 0.6f;

    public MainNavViewPager(Context context) {
        super(context);
    }

    public MainNavViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        int height = getHeight();
        if (height <= 0) {
            return super.onInterceptTouchEvent(ev);
        }

        float touchY = ev.getY();
        float threshold = height * SWIPE_THRESHOLD;

        if (touchY <= threshold) {
            return false;
        }

        return super.onInterceptTouchEvent(ev);
    }
}