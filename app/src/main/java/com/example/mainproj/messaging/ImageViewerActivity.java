package com.example.mainproj.messaging;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.mainproj.R;
import com.github.chrisbanes.photoview.PhotoView;

public class ImageViewerActivity extends AppCompatActivity {

    public static final String EXTRA_IMAGE_URL = "imageUrl";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_viewer);

        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        String imageUrl = getIntent().getStringExtra(EXTRA_IMAGE_URL);
        if (imageUrl == null || imageUrl.isEmpty()) {
            finish();
            return;
        }

        PhotoView photoView = findViewById(R.id.photoView);

        if (imageUrl.startsWith("http")) {
            Glide.with(this)
                    .load(imageUrl)
                    .fitCenter()
                    .into(photoView);
        } else {
            try {
                byte[] decodedBytes = Base64.decode(imageUrl, Base64.DEFAULT);
                Bitmap bitmap = decodeSampledBitmap(decodedBytes, 2048, 2048); // desired (width, height)
                photoView.setImageBitmap(bitmap);
            } catch (Throwable e) {
                finish();
            }
        }
    }

    private Bitmap decodeSampledBitmap(byte[] data, int reqWidth, int reqHeight) {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length, opts);
        opts.inSampleSize = calculateInSampleSize(opts, reqWidth, reqHeight);
        opts.inJustDecodeBounds = false;
        return BitmapFactory.decodeByteArray(data, 0, data.length, opts);
    }

    private int calculateInSampleSize(BitmapFactory.Options opts, int reqWidth, int reqHeight) {
        int height = opts.outHeight;
        int width = opts.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            int halfHeight = height / 2;
            int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }
}
