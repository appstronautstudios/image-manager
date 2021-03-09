package com.appstronautstudios.imagemanagerdemo;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.appstronautstudios.imagemanager.managers.ImageManager;
import com.appstronautstudios.imagemanager.utils.SuccessFailListener;
import com.example.imagemanager.R;

public class MainActivity extends AppCompatActivity {

    private final String IMAGE_FOLDER = "Image Manager Demo";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final LinearLayout contentLL = findViewById(R.id.content_layout);

        Drawable[] drawables = new Drawable[]{
                getResources().getDrawable(R.drawable.forest_1),
                getResources().getDrawable(R.drawable.forest_2),
                getResources().getDrawable(R.drawable.forest_3),
                getResources().getDrawable(R.drawable.forest_4),
                getResources().getDrawable(R.drawable.forest_5),
                getResources().getDrawable(R.drawable.forest_6),
                getResources().getDrawable(R.drawable.forest_7),
                getResources().getDrawable(R.drawable.forest_8),
                getResources().getDrawable(R.drawable.forest_9),
                getResources().getDrawable(R.drawable.forest_10),
                getResources().getDrawable(R.drawable.forest_11),
                getResources().getDrawable(R.drawable.forest_12)
        };

        for (Drawable drawable : drawables) {
            contentLL.addView(createImageCell(drawable));
        }
    }

    private View createImageCell(Drawable drawable) {
        final View root = getLayoutInflater().inflate(R.layout.view_image_cell, null, false);
        final ImageView imageView = root.findViewById(R.id.image_view);
        Button imageShareBTN = root.findViewById(R.id.image_share);
        Button imageSaveBTN = root.findViewById(R.id.image_save);
        Button viewSaveBTN = root.findViewById(R.id.view_save);

        imageView.setImageDrawable(drawable);
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
                ImageManager.getInstance().saveToGallery(MainActivity.this, bmp, IMAGE_FOLDER, "forest_" + System.currentTimeMillis(), new SuccessFailListener() {
                    @Override
                    public void success(Object object) {
                        Toast.makeText(MainActivity.this, "Image saved successfully", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void failure(Object object) {
                        Toast.makeText(MainActivity.this, ((Exception) object).getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        viewSaveBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bitmap bmp = ImageManager.getInstance().viewToBitmap(root);
                ImageManager.getInstance().saveToGallery(MainActivity.this, bmp, IMAGE_FOLDER, "layout", new SuccessFailListener() {
                    @Override
                    public void success(Object object) {
                        Toast.makeText(MainActivity.this, "View saved successfully", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void failure(Object object) {
                        Toast.makeText(MainActivity.this, ((Exception) object).getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        return root;
    }
}
