package com.example.prashant.doodle;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    FrameLayout drawingPanel;
    ImageButton takePicture;
    ImageButton choosePicture;
    ImageButton savePicture;
    ImageButton undoButton;
    ImageButton redoButton;
    LinearLayout optionContainer;

    static final int REQUEST_CHOOSE_IMAGE = 1;
    static final int REQUEST_IMAGE_CAPTURE = 2;

    Bitmap alteredBitmap;
    DrawingPanel dp;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        drawingPanel = (FrameLayout) this.findViewById(R.id.DrawingPanel);
        choosePicture = (ImageButton) this.findViewById(R.id.ChoosePictureButton);
        savePicture = (ImageButton) this.findViewById(R.id.SavePictureButton);
        takePicture = (ImageButton) this.findViewById(R.id.TakePictureButton);
        undoButton = (ImageButton) this.findViewById(R.id.UndoPictureButton);
        redoButton = (ImageButton) this.findViewById(R.id.RedoPictureButton);
        optionContainer = (LinearLayout) this.findViewById(R.id.OptionContainer);


        savePicture.setOnClickListener(this);
        choosePicture.setOnClickListener(this);
        takePicture.setOnClickListener(this);
        undoButton.setOnClickListener(this);
        redoButton.setOnClickListener(this);
        isStoragePermissionGranted();
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA))
            takePicture.setEnabled(false);
        dp = new DrawingPanel(this);
        drawingPanel.addView(dp);
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ChoosePictureButton:
                dispatchChooseImageIntent();
                break;
            case R.id.SavePictureButton:
                saveFinalAlteredImage();
                break;
            case R.id.TakePictureButton:
                dispatchTakePictureIntent();
                break;
            case R.id.UndoPictureButton:
                onClickUndo();
                break;
            case R.id.RedoPictureButton:
                onClickRedo();
            default:
                break;
        }
    }

    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        switch (requestCode) {
            case REQUEST_CHOOSE_IMAGE:
                if (resultCode == RESULT_OK) {
                    Uri imageFileUri = intent.getData();
                    try {
                        Bitmap bmp = decodeUri(imageFileUri);
                        Drawable d = new BitmapDrawable(getResources(), bmp);
                        dp.setBackground(d);
                        switchToDrawingMode();
                    } catch (Exception e) {
                        Log.v("ERROR", e.toString());
                    }
                }
                break;
            case REQUEST_IMAGE_CAPTURE:
                if (resultCode == RESULT_OK) {
                    Bundle extras = intent.getExtras();
                    Bitmap bmp = (Bitmap) extras.get("data");
                    Drawable d = new BitmapDrawable(getResources(), bmp);
                    dp.setBackground(d);
                    switchToDrawingMode();
                }
                break;
        }
    }

    private void switchToDrawingMode() {
        optionContainer.setVisibility(View.GONE);
        drawingPanel.setVisibility(View.VISIBLE);
    }

    public boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                return true;
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        } else { //permission is automatically granted on sdk<23 upon installation
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            //resume tasks needing this permission
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    private void dispatchChooseImageIntent() {
        Intent choosePictureIntent = new Intent(
                Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        if (choosePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(choosePictureIntent, REQUEST_CHOOSE_IMAGE);
        }
    }

    private void saveFinalAlteredImage() {
        alteredBitmap = getBitmapFromView(dp);
        if (alteredBitmap != null) {
            ContentValues contentValues = new ContentValues(3);
            contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, "Draw On Me");
            Uri imageFileUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
            try {
                OutputStream imageFileOS = getContentResolver().openOutputStream(imageFileUri);
                alteredBitmap.compress(Bitmap.CompressFormat.JPEG, 90, imageFileOS);
                Toast t = Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT);
                t.show();
            } catch (Exception e) {
                Log.v("EXCEPTION", e.getMessage());
            }
        }
    }

    private Bitmap decodeUri(Uri selectedImage) throws FileNotFoundException {
        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(getContentResolver().openInputStream(selectedImage), null, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;
        int targetW = 300;
        int targetH = 300;

        // Determine how much to scale down the image
        int scaleFactor = Math.min(photoW / targetW, photoH / targetH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        return BitmapFactory.decodeStream(getContentResolver().openInputStream(selectedImage), null, bmOptions);
    }


    public void onClickUndo() {
        dp.onClickUndo();
    }

    public void onClickRedo() {
        dp.onClickRedo();
    }

    public static Bitmap getBitmapFromView(View view) {
        //Define a bitmap with the same size as the view
        Bitmap returnedBitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        //Bind a canvas to it
        Canvas canvas = new Canvas(returnedBitmap);
        //Get the view's background
        Drawable bgDrawable = view.getBackground();
        if (bgDrawable != null)
            //has background drawable, then draw it on the canvas
            bgDrawable.draw(canvas);
        else
            //does not have background drawable, then draw white background on the canvas
            canvas.drawColor(Color.WHITE);
        // draw the view on the canvas
        view.draw(canvas);
        //return the bitmap
        return returnedBitmap;
    }
}