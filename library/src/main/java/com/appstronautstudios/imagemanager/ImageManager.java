package com.appstronautstudios.imagemanager;

import android.content.ContentValues;
import android.content.Context;
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
import android.widget.Toast;

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
     * @param context   context
     * @param bitmap    bitmap to save
     * @param albumName album name to save to in pictures directory
     * @param fileName  name of file without extension
     * @return uri of file saved
     */
    public Uri saveToGallery(Context context, Bitmap bitmap, String albumName, String fileName) {
        // https://proandroiddev.com/working-with-scoped-storage-8a7e7cafea3
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/" + albumName);
            Uri collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
            Uri uri = context.getContentResolver().insert(collection, contentValues);
            if (uri != null) {
                try {
                    OutputStream out = context.getContentResolver().openOutputStream(uri);
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
            return saveToGalleryLegacy(context, bitmap, albumName, fileName);
        }
    }

    /**
     * WARNING - does not work on android 10+
     *
     * @param context   context
     * @param bitmap    bitmap to save
     * @param albumName album name to save to in pictures directory
     * @param fileName  name of file without extension
     * @return uri of file saved
     */
    public Uri saveToGalleryLegacy(Context context, Bitmap bitmap, String albumName, String fileName) {
        // create image folder if does not exist
        File imagesFolder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), albumName);
        if (!imagesFolder.mkdirs() && !imagesFolder.isDirectory()) {
            String state = Environment.getExternalStorageState();
            if (Environment.MEDIA_MOUNTED.equals(state)) {
                // failed to create and is not a directory. Something went wrong...
            } else {
            }
        }

        // delete image if already exists so FOS can create a new one
        File image = new File(imagesFolder, fileName + ".jpg");
        if (image.exists()) {
            // image already exists, deleting to start from clean state
            if (!image.delete()) {
                // failed to delete
            }
        }

        // compress bitmap and write to file stream. FOS creates file if does not exist
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(image);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, out);
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // get Uri from saved image
        Uri uriSavedImage = Uri.fromFile(image);

        // media scan the new file so it shows up in the gallery
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(uriSavedImage);
        context.sendBroadcast(mediaScanIntent);

        // inform the user the image save is complete
        Toast.makeText(context, "Image Saved", Toast.LENGTH_LONG).show();

        return uriSavedImage;
    }
}
