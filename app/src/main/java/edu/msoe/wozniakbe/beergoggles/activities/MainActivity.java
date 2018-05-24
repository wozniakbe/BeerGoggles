package edu.msoe.wozniakbe.beergoggles.activities;

import android.Manifest;
import android.app.Activity;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.app.ListActivity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
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
import java.util.Locale;

import edu.msoe.wozniakbe.beergoggles.BuildConfig;
import edu.msoe.wozniakbe.beergoggles.R;
import edu.msoe.wozniakbe.beergoggles.fragments.BeerListFragment;
import edu.msoe.wozniakbe.beergoggles.src.Beer;

import static android.content.ContentValues.TAG;

/**
 * Author: Ben Wozniak (wozniakbe@msoe.edu)
 * Main activity which allows users to search for a beer via text, camera, or voice
 */

public class MainActivity extends AppCompatActivity implements BeerListFragment.OnListFragmentInteractionListener {

    private DatabaseReference databaseReference;
    private final String BEERS_PATH = "beers";
    private ArrayList<Beer> beers;

    private EditText searchText;
    private Button searchButton;
    private Button cameraButton;
    private Button voiceButton;
    private TextView ocrText;

    private TessBaseAPI tessBaseAPI;
    private static final String TESS_DATA = "/tessdata";
    private String mCurrentPhotoPath;

    private final int REQ_CODE_SPEECH_INPUT = 100;
    private final int REQ_CODE_CAMERA = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeGui();

        beers = new ArrayList<>();
        beers.add(new Beer("Name", "IBU", "ABV"));

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction ft = fragmentManager.beginTransaction();

        Bundle bundle = new Bundle();
        bundle.putSerializable("beers", beers);
        BeerListFragment beerListFragment = new BeerListFragment();
        beerListFragment.setArguments(bundle);

        ft.add(R.id.beerListPlaceholder, BeerListFragment.newInstance(beers));
        ft.commit();

        databaseReference = FirebaseDatabase.getInstance().getReference(BEERS_PATH);
    }

    /**
     * Initialize GUI components as java objects
     */
    private void initializeGui(){
        this.searchText = findViewById(R.id.searchText);
        this.searchButton = findViewById(R.id.searchButton);
        this.searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSearchClicked();
            }
        });
        this.cameraButton = findViewById(R.id.cameraButton);
        this.cameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onCameraClicked();
            }
        });
        this.voiceButton = findViewById(R.id.voiceButton);
        this.voiceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchSpeechInputIntent();
            }
        });
        this.ocrText = findViewById(R.id.ocrText);
    }

    /**
     * Launches a new intent which accesses the device's native camera
     * and returns the picture taken
     */
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
                startActivityForResult(takePictureIntent, REQ_CODE_CAMERA);
            }
        }
    }

    // Showing google speech input dialog

    private void dispatchSpeechInputIntent() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                getString(R.string.speechPrompt));
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {

        }
    }

    /**
     * Creates an image file and stores in a temporary directory
     * @return Returns the file for the image
     * @throws IOException
     */
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
    /**
     * Used to receive the result of the take picture intent,
     * then imports the tess language data to the device's native storage.
     * Finally, starts optical character recognition (OCR) on the image text
     */
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_CODE_CAMERA) {
            if (resultCode == Activity.RESULT_OK) {
                prepareTessData();
                startOCR();
            } else if (resultCode == Activity.RESULT_CANCELED) {
                Toast.makeText(getApplicationContext(), "Result canceled.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(), "Activity result failed.", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQ_CODE_SPEECH_INPUT) {
            if (resultCode == RESULT_OK && data != null) {

                ArrayList<String> result = data
                        .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                searchText.setText(result.get(0));
            }
        }
    }

    /**
     * Transforms the image file into a bitmap which can be consumed
     * by the tess-two API
     */
    private void startOCR(){
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

                for (DataSnapshot child: dataSnapshot.getChildren()) {
                    beers.add(child.getValue(Beer.class));
//                    beerNames.add(child.getValue(Beer.class).getName());
                }
               // adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        };
        databaseReference.orderByChild("name").startAt(searchText.getText().toString()).endAt(searchText.getText().toString()).addListenerForSingleValueEvent(beerListener);

    }

    /**
     * When camera icon is clicked, start the take picture intent
     */
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
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private String getText(Bitmap bitmap){
        try{
            tessBaseAPI = new TessBaseAPI();
        }catch (Exception e){
            Log.e(TAG, e.getMessage());
        }
        String dataPath = getExternalFilesDir("/").getPath() + "/";
        tessBaseAPI.init(dataPath,"eng");
        tessBaseAPI.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ");
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

    @Override
    public void onListFragmentInteraction(Beer beer) {

    }
}
