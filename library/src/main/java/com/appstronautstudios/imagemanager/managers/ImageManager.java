package com.appstronautstudios.imagemanager.managers;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;

import androidx.core.content.FileProvider;

import com.appstronautstudios.imagemanager.utils.SuccessFailListener;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class ImageManager {

    private static final ImageManager INSTANCE = new ImageManager();

    private ImageManager() {
        if (INSTANCE != null) {
            throw new IllegalStateException("Already instantiated");
        }
    }

    public static ImageManager getInstance() {
        return INSTANCE;
    }

    /**
     * get bitmap from provided image view
     *
     * @param imageView ImageView to fetch bitmap from
     * @return bitmap
     */
    public Bitmap getBitmapFromImageView(ImageView imageView) {
        return ((BitmapDrawable) imageView.getDrawable()).getBitmap();
    }

    /**
     * get bitmap from any view. Think screenshot
     *
     * @return bitmap
     */
    public Bitmap viewToBitmap(View view) {
        // define a bitmap with the same size as the view
        Bitmap returnedBitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_4444);
        // bind a canvas to it
        Canvas canvas = new Canvas(returnedBitmap);
        // set background
        Drawable bgDrawable = view.getBackground();
        if (bgDrawable != null) {
            // has background drawable, then draw it on the canvas
            bgDrawable.draw(canvas);
        } else {
            // does not have background drawable, then draw white background on the canvas
            canvas.drawColor(Color.TRANSPARENT);
        }
        // draw the view on the canvas
        view.draw(canvas);
        // return the bitmap
        return returnedBitmap;
    }

    /**
     * writes image to cache and shares with an intent flag permission circumventing requirement to
     * write to external storage
     *
     * @param activity activity
     * @param bitmap   bitmap to share
     */
    public void shareImageWithoutPermissions(Activity activity, Bitmap bitmap) {
        // create file and folder in app specific cache
        File cacheFolder = new File(activity.getCacheDir(), "images");
        cacheFolder.mkdirs(); // don't forget to make the directory
        File cacheImage = new File(cacheFolder, "temp_image.png");

        // write our bitmap to file
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(cacheImage); // overwrites this image every time
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // get content URI of cached image and send share intent with temp read URI permission
        // https://developer.android.com/reference/android/support/v4/content/FileProvider.html#Permissions
        Uri contentUri = FileProvider.getUriForFile(activity, "com.appstronautstudios.fileprovider", cacheImage);
        activity.grantUriPermission(activity.getPackageName(), contentUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        if (contentUri != null) {
            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // temp permission for receiving app to read this file
            shareIntent.setDataAndType(contentUri, activity.getContentResolver().getType(contentUri));
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            activity.startActivity(Intent.createChooser(shareIntent, "Choose an app"));
        }
    }

    /**
     * @param activity  activity
     * @param bitmap    bitmap to save
     * @param albumName album name to save to in pictures directory
     * @param fileName  name of file without extension
     */
    public void saveToGallery(final Activity activity, final Bitmap bitmap, final String albumName, final String fileName) {
        saveToGallery(activity, bitmap, albumName, fileName, null);
    }

    /**
     * @param activity  activity
     * @param bitmap    bitmap to save
     * @param albumName album name to save to in pictures directory
     * @param fileName  name of file without extension
     * @param listener  success/fail listener
     */
    public void saveToGallery(final Activity activity, final Bitmap bitmap, final String albumName, final String fileName, final SuccessFailListener listener) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // saving to scoped storage on android 29+ does not require write_external
            // https://developer.android.com/training/data-storage/shared/media
            Uri uri = saveToGalleryWithoutPermissionCheck(activity, bitmap, albumName, fileName);
            if (listener != null) {
                if (uri != null) {
                    listener.success(uri);
                } else {
                    listener.failure(new Exception("Save failure"));
                }
            }
        } else {
            Dexter.withContext(activity)
                    .withPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    .withListener(new PermissionListener() {
                        @Override
                        public void onPermissionGranted(PermissionGrantedResponse response) {
                            Uri uri = saveToGalleryWithoutPermissionCheck(activity, bitmap, albumName, fileName);
                            if (listener != null) {
                                if (uri != null) {
                                    listener.success(uri);
                                } else {
                                    listener.failure(new Exception("Save failure"));
                                }
                            }
                        }

                        @Override
                        public void onPermissionDenied(PermissionDeniedResponse response) {
                            if (response.isPermanentlyDenied()) {
                                if (listener != null) {
                                    listener.failure(new SecurityException("Permission denied"));
                                }
                            }
                        }

                        @Override
                        public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
                            token.continuePermissionRequest();
                        }
                    }).check();
        }
    }

    /**
     * @param activity  context
     * @param bitmap    bitmap to save
     * @param albumName album name to save to in pictures directory
     * @param fileName  name of file without extension
     * @return uri of file saved
     */
    private Uri saveToGalleryWithoutPermissionCheck(final Activity activity, final Bitmap bitmap, final String albumName, final String fileName) {
        // https://proandroiddev.com/working-with-scoped-storage-8a7e7cafea3
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/" + albumName);
            Uri collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
            Uri uri = activity.getContentResolver().insert(collection, contentValues);
            if (uri != null) {
                try {
                    OutputStream out = activity.getContentResolver().openOutputStream(uri);
                    if (out != null) {
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
                        out.close();
                    }
                    return uri;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return null;
        } else {
            return saveToGalleryLegacyWithoutPermissionCheck(activity, bitmap, albumName, fileName);
        }
    }

    /**
     * WARNING - does not work on android 10+
     *
     * @param activity  context
     * @param bitmap    bitmap to save
     * @param albumName album name to save to in pictures directory
     * @param fileName  name of file without extension
     * @return uri of file saved
     */
    private Uri saveToGalleryLegacyWithoutPermissionCheck(Activity activity, Bitmap bitmap, String albumName, String fileName) {
        // create image folder if does not exist
        File imagesFolder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), albumName);
        if (!imagesFolder.mkdirs() && !imagesFolder.isDirectory()) {
            String state = Environment.getExternalStorageState();
            if (Environment.MEDIA_MOUNTED.equals(state)) {
                // failed to create and is not a directory. Something went wrong...
                return null;
            }
        }

        // delete image if already exists so FOS can create a new one
        File image = new File(imagesFolder, fileName + ".jpg");
        if (image.exists()) {
            // image already exists, deleting to start from clean state
            if (!image.delete()) {
                // failed to delete
                return null;
            }
        }

        // compress bitmap and write to file stream. FOS creates file if does not exist
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(image);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        // get Uri from saved image
        Uri uriSavedImage = Uri.fromFile(image);

        // media scan the new file so it shows up in the gallery
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(uriSavedImage);
        activity.sendBroadcast(mediaScanIntent);

        return uriSavedImage;
    }
}
