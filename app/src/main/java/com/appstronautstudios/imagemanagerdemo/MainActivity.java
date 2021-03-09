package com.appstronautstudios.imagemanagerdemo;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.appstronautstudios.imagemanager.ImageManager;
import com.example.imagemanager.R;

public class MainActivity extends AppCompatActivity {

    private final String IMAGE_FOLDER = "Image Manager Demo";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final View parent = findViewById(R.id.parent);
        final ImageView imageView = findViewById(R.id.image_view);
        Button imageShareBTN = findViewById(R.id.image_share);
        Button imageSaveBTN = findViewById(R.id.image_save);
        Button viewSaveBTN = findViewById(R.id.view_save);

        imageShareBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bitmap bmp = ImageManager.getInstance().getBitmapFromImageView(imageView);
                ImageManager.getInstance().shareImageWithoutPermissions(MainActivity.this, bmp);
            }
        });
        imageSaveBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bitmap bmp = ImageManager.getInstance().getBitmapFromImageView(imageView);
                ImageManager.getInstance().saveToGallery(MainActivity.this, bmp, IMAGE_FOLDER, "forest");
            }
        });
        viewSaveBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bitmap bmp = ImageManager.getInstance().viewToBitmap(parent);
                ImageManager.getInstance().saveToGallery(MainActivity.this, bmp, IMAGE_FOLDER, "layout");
            }
        });
    }
}
