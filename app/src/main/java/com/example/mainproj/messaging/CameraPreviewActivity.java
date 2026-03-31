package com.example.mainproj.messaging;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.example.mainproj.R;
import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CameraPreviewActivity extends AppCompatActivity {

    public static final String EXTRA_IMAGE_URI = "extra_image_uri";
    public static final String RESULT_IMAGE_URI = "result_image_uri";

    private ImageView previewImage;
    private ImageButton btnClose;
    private ImageButton btnMoreMenu;
    private MaterialButton btnSend;

    private Uri imageUri;
    private Uri savedImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_preview);

        previewImage = findViewById(R.id.previewImage);
        btnClose = findViewById(R.id.btnClose);
        btnMoreMenu = findViewById(R.id.btnMoreMenu);
        btnSend = findViewById(R.id.btnSend);

        String uriString = getIntent().getStringExtra(EXTRA_IMAGE_URI);
        if (uriString != null) {
            imageUri = Uri.parse(uriString);
            loadPreview(imageUri);
        } else {
            Toast.makeText(this, "No image received", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        btnClose.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });

        btnSend.setOnClickListener(v -> {
            Intent resultIntent = new Intent();
            resultIntent.putExtra(RESULT_IMAGE_URI, imageUri.toString());
            setResult(RESULT_OK, resultIntent);
            finish();
        });

        btnMoreMenu.setOnClickListener(v -> showMoreMenu(v));
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
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void showMoreMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenuInflater().inflate(R.menu.menu_camera_preview, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_save_to_gallery) {
                saveToGallery();
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void saveToGallery() {
        if (imageUri == null) {
            Toast.makeText(this, "No image to save", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "IMG_" + timeStamp + ".jpg";

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_DCIM + "/CameraApp");
                values.put(MediaStore.Images.Media.IS_PENDING, 1);

                Uri collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
                Uri savedUri = getContentResolver().insert(collection, values);

                if (savedUri != null) {
                    InputStream inputStream = getContentResolver().openInputStream(imageUri);
                    if (inputStream != null) {
                        FileOutputStream outputStream = (FileOutputStream) getContentResolver().openOutputStream(savedUri);
                        if (outputStream != null) {
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                            outputStream.close();
                            bitmap.recycle();
                        }
                        inputStream.close();
                    }

                    values.clear();
                    values.put(MediaStore.Images.Media.IS_PENDING, 0);
                    getContentResolver().update(savedUri, values, null, null);

                    savedImageUri = savedUri;
                    Toast.makeText(this, "Saved to Gallery", Toast.LENGTH_SHORT).show();
                }
            } else {
                File picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
                File cameraAppDir = new File(picturesDir, "CameraApp");
                if (!cameraAppDir.exists()) {
                    cameraAppDir.mkdirs();
                }

                File imageFile = new File(cameraAppDir, fileName);

                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                if (inputStream != null) {
                    FileOutputStream outputStream = new FileOutputStream(imageFile);
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                    outputStream.close();
                    inputStream.close();
                    bitmap.recycle();

                    savedImageUri = Uri.fromFile(imageFile);
                    Toast.makeText(this, "Saved to Gallery", Toast.LENGTH_SHORT).show();

                    Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                    mediaScanIntent.setData(Uri.fromFile(imageFile));
                    sendBroadcast(mediaScanIntent);
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
}
