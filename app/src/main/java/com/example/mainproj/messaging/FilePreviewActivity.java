package com.example.mainproj.messaging;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mainproj.R;
import com.google.android.material.button.MaterialButton;

import java.io.InputStream;

public class FilePreviewActivity extends AppCompatActivity {

    public static final String EXTRA_FILE_URI = "extra_file_uri";
    public static final String RESULT_FILE_URI = "result_file_uri";

    private ImageView previewImage;
    private ImageButton btnClose;
    private ImageButton btnMoreMenu;
    private MaterialButton btnSend;

    private Uri fileUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_preview);

        previewImage = findViewById(R.id.previewImage);
        btnClose = findViewById(R.id.btnClose);
        btnMoreMenu = findViewById(R.id.btnMoreMenu);
        btnSend = findViewById(R.id.btnSend);

        String uriString = getIntent().getStringExtra(EXTRA_FILE_URI);
        if (uriString != null) {
            fileUri = Uri.parse(uriString);
            loadPreview(fileUri);
        } else {
            Toast.makeText(this, "No file received", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        btnClose.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });

        btnSend.setOnClickListener(v -> {
            Intent resultIntent = new Intent();
            resultIntent.putExtra(RESULT_FILE_URI, fileUri.toString());
            setResult(RESULT_OK, resultIntent);
            finish();
        });

        btnMoreMenu.setVisibility(View.GONE);
    }

    private void loadPreview(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (inputStream != null) {
                inputStream.close();
            }
            previewImage.setImageBitmap(bitmap);
        } catch (Exception e) {
            Toast.makeText(this, "Failed to load file", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
}
