package edu.msoe.wozniakbe.beergoggles.activities;

import android.Manifest;
import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import edu.msoe.wozniakbe.beergoggles.BuildConfig;
import edu.msoe.wozniakbe.beergoggles.R;
import edu.msoe.wozniakbe.beergoggles.src.Beer;

import static android.content.ContentValues.TAG;

public class MainActivity extends ListActivity {

    private DatabaseReference databaseReference;
    private final String BEERS_PATH = "beers";
    private ArrayList<Beer> beers;
    private ArrayList<String> beerNames;

    private EditText searchText;
    private Button searchButton;
    private TextView ocrText;
    private ArrayAdapter<String> adapter; // TODO: Change all this to beer object

    private TessBaseAPI tessBaseAPI;
    public static final String DATA_PATH = Environment.getExternalStorageDirectory().toString() + "/BeerGoggles/";
    private static final String TESS_DATA = "/tessdata";
    private String mCurrentPhotoPath;
    private Uri outputFileDir;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeGui();

        beers = new ArrayList<>();
        beerNames = new ArrayList<>();

        adapter=new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                beerNames);

        setListAdapter(adapter);
        databaseReference = FirebaseDatabase.getInstance().getReference(BEERS_PATH);
    }

    private void initializeGui(){
        this.searchText = findViewById(R.id.searchText);
        this.searchButton = findViewById(R.id.searchButton);
        this.searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSearchClicked();
            }
        });
        this.ocrText = findViewById(R.id.ocrText);
    }

    private void checkPermission() {
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 120);
        }
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 121);
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        BuildConfig.APPLICATION_ID + ".provider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, 1024);
            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1024) {
            if (resultCode == Activity.RESULT_OK) {
                prepareTessData();
                startOCR(outputFileDir);
            } else if (resultCode == Activity.RESULT_CANCELED) {
                Toast.makeText(getApplicationContext(), "Result canceled.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(), "Activity result failed.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startOCR(Uri imageUri){
        try{
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = false;
            options.inSampleSize = 6;
            Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, options);
            String result = this.getText(bitmap);
            ocrText.setText(result);
        }catch (Exception e){
            Log.e(TAG, e.getMessage());
        }
    }

    private void onSearchClicked(){
        ValueEventListener beerListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                beers.clear();

                for (DataSnapshot child: dataSnapshot.getChildren()) {
                    beers.add(child.getValue(Beer.class));
                    beerNames.add(child.getValue(Beer.class).getName());
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Getting Post failed, log a message
                //Log.w(TAG, "loadPost:onCancelled", databaseError.toException());
                // ...
            }
        };
        databaseReference.orderByChild("name").startAt(searchText.getText().toString()).endAt(searchText.getText().toString()).addListenerForSingleValueEvent(beerListener);

    }

    private void onCameraClicked(){
        prepareTessData();
        dispatchTakePictureIntent();
    }

    /**
     * This method sets up the tess API and gives it the bitmap image taken by the user's camera.
     * It then attempts to get the text via the get UTF8Text() method
     * @param bitmap The bitmap of the photo taken which is to be identified
     * @return The text recognized by tess-two OCR from the image
     */
    private String getText(Bitmap bitmap){
        try{
            tessBaseAPI = new TessBaseAPI();
        }catch (Exception e){
            Log.e(TAG, e.getMessage());
        }
        String dataPath = getExternalFilesDir("/").getPath() + "/";
        tessBaseAPI.init(dataPath,"eng");
        tessBaseAPI.setVariable("tessedit_char_whitelist", "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ");
        tessBaseAPI.setImage(bitmap);
        String retStr = "No result";
        try{
            retStr = tessBaseAPI.getUTF8Text();
        }catch (Exception e){
            Log.e(TAG, e.getMessage());
        }
        tessBaseAPI.end();
        return retStr;
    }

    /**
     * TODO: PROBABLY UNUSED. DELETE THIS METHOD IF TRULY UNUSED
     * @param mgr
     * @param path
     * @return
     */
    public static Bitmap getBitmapFromAsset(AssetManager mgr, String path) {
        InputStream is = null;
        Bitmap bitmap = null;
        try {
            is = mgr.open(path);
            bitmap = BitmapFactory.decodeStream(is);
        } catch (final IOException e) {
            bitmap = null;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ignored) {
                }
            }
        }
        return bitmap;
    }

    /**
     * Imports tess-two english language data file into the device's main storage if it does not already exist.
     * This allows the OCR to happen.
     */
    private void prepareTessData(){
        try{
            File dir = getExternalFilesDir(TESS_DATA);
            if(!dir.exists()){
                if (!dir.mkdir()) { // Makes tess directory if it does not already exist
                    Toast.makeText(getApplicationContext(), "ERROR: The folder " + dir.getPath() + "was not created", Toast.LENGTH_SHORT).show();
                }
            }

            // Copies tess language data from asset file onto device's local memory
            String fileList[] = getAssets().list("");
            for(String fileName : fileList){
                String pathToDataFile = dir + "/" + fileName;
                if(!(new File(pathToDataFile)).exists()){
                    InputStream in = getAssets().open(fileName);
                    OutputStream out = new FileOutputStream(pathToDataFile);
                    byte [] buff = new byte[1024];
                    int len ;
                    while(( len = in.read(buff)) > 0){
                        out.write(buff,0,len);
                    }
                    in.close();
                    out.close();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }
}
