package com.appstronautstudios.imagemanagerdemo;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.text.HtmlCompat;

import com.appstronautstudios.imagemanager.managers.ImageManager;
import com.appstronautstudios.imagemanager.utils.SuccessFailListener;
import com.example.imagemanager.R;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.SortedSet;
import java.util.TreeSet;

public class MainActivity extends AppCompatActivity {

    private final String IMAGE_FOLDER = "Image Manager Demo";
    private static final int CAMERA_REQUEST = 1888;
    private Uri currentUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Drawable[] drawables = new Drawable[]{
                ContextCompat.getDrawable(this, R.drawable.forest_1),
                ContextCompat.getDrawable(this, R.drawable.forest_2),
                ContextCompat.getDrawable(this, R.drawable.forest_3),
                ContextCompat.getDrawable(this, R.drawable.forest_4),
                ContextCompat.getDrawable(this, R.drawable.forest_5),
                ContextCompat.getDrawable(this, R.drawable.forest_6),
                ContextCompat.getDrawable(this, R.drawable.forest_7),
                ContextCompat.getDrawable(this, R.drawable.forest_8),
                ContextCompat.getDrawable(this, R.drawable.forest_9),
                ContextCompat.getDrawable(this, R.drawable.forest_10),
                ContextCompat.getDrawable(this, R.drawable.forest_11),
                ContextCompat.getDrawable(this, R.drawable.forest_12)
        };

        // set up onactivityresult
        ActivityResultLauncher<Intent> resultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            Intent data = result.getData();
                            try {
                                if (data == null) throw (new Exception("null data"));
                                Uri uri = data.getData();
                                if (uri == null) throw (new Exception("null uri"));
                                // combine hash set entries into something readable
                                ArrayList<String> lines = new ArrayList<>();
                                HashMap<String, String> exifData = ImageManager.getAllExifData(getContentResolver().openInputStream(uri));
                                SortedSet<String> keys = new TreeSet<>(exifData.keySet());
                                for (String key : keys) {
                                    String value = exifData.get(key);
                                    lines.add("<b>" + key + "</b><br/>" + TextUtils.htmlEncode(value));
                                }
                                new AlertDialog.Builder(MainActivity.this)
                                        .setTitle("EXIF data")
                                        .setMessage(HtmlCompat.fromHtml(String.join("<br><br>", lines), HtmlCompat.FROM_HTML_MODE_LEGACY))
                                        .setPositiveButton("Close", null)
                                        .create()
                                        .show();
                            } catch (Exception e) {
                                new AlertDialog.Builder(MainActivity.this)
                                        .setTitle("File not readable")
                                        .setMessage("The file you have selected is not an image or is not readable by this project")
                                        .setPositiveButton("Close", null)
                                        .create()
                                        .show();
                            }
                        }
                    }
                });
        ActivityResultLauncher<Intent> pictureLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            Intent data = result.getData();
                            try {
                                ImageManager.getInstance().saveToGallery(
                                        MainActivity.this,
                                        MediaStore.Images.Media.getBitmap(getContentResolver(), currentUri),
                                        IMAGE_FOLDER,
                                        currentUri.getLastPathSegment());
                                new AlertDialog.Builder(MainActivity.this)
                                        .setTitle("Image saved!")
                                        .setMessage("Your image has been captured and saved to the provided URI")
                                        .setPositiveButton("Close", null)
                                        .create()
                                        .show();
                            } catch (Exception e) {
                                new AlertDialog.Builder(MainActivity.this)
                                        .setTitle("Error")
                                        .setMessage("Something went wrong while trying to save the image")
                                        .setPositiveButton("Close", null)
                                        .create()
                                        .show();
                            }
                        }
                    }
                });

        // set up top buttons
        findViewById(R.id.btn_photo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Dexter.withContext(MainActivity.this)
                        .withPermission(Manifest.permission.CAMERA)
                        .withListener(new PermissionListener() {
                            @Override
                            public void onPermissionGranted(PermissionGrantedResponse response) {
                                File file = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), System.currentTimeMillis() + ".jpg");
                                currentUri = FileProvider.getUriForFile(MainActivity.this, getPackageName() + ".imagemanager.shareprovider", file);
                                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentUri);
                                pictureLauncher.launch(takePictureIntent);
                            }

                            @Override
                            public void onPermissionDenied(PermissionDeniedResponse response) {
                                if (response.isPermanentlyDenied()) {
                                    Toast.makeText(MainActivity.this, "Camera permission required to add photo to log", Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
                                token.continuePermissionRequest();
                            }
                        }).check();

//
//                ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(getContentResolver(), MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
//                        .build();
//                imageCapture.takePicture(
//                        outputOptions,
//                        ContextCompat.getMainExecutor(MainActivity.this),
//                        new ImageCapture.OnImageSavedCallback() {
//                            @Override
//                            public void onImageSaved(ImageCapture.OutputFileResults outputFileResults) {
//                                // insert your code here.
//                            }
//
//                            @Override
//                            public void onError(ImageCaptureException error) {
//                                // insert your code here.
//                            }
//                        }
//                );
            }
        });

        findViewById(R.id.btn_exif).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
                chooseFile.setType("image/*");
                chooseFile.setAction(Intent.ACTION_GET_CONTENT);
                resultLauncher.launch(chooseFile);
            }
        });

        // set up images
        final LinearLayout contentLL = findViewById(R.id.content_layout);
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
