package com.example.deanf.argallery;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.TypefaceSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;


public class MainActivity extends AppCompatActivity {
    final private int PICK_FILE_REQUEST = 1253;
    final private int READ_PERMISSION_REQUEST = 1324;

    private RelativeLayout layout;
    private ImageView imageView;
    private TextView textView;
    private Button btnChooseFile;
    private Button btnRetry;

    private String filePath;
    private Boolean canLaunch = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Update font of action bar
        SpannableString s = new SpannableString(getString(R.string.app_name));
        s.setSpan(new TypefaceSpan("raleway_bold.ttf"), 0, s.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(s);

        // Get component references
        layout = findViewById(R.id.main_layout);
        imageView = findViewById(R.id.img_main);
        textView = findViewById(R.id.txtHint);
        btnChooseFile = findViewById(R.id.btn_main);
        btnRetry = findViewById(R.id.btn_retry);

        // Button click listeners
        btnChooseFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!canLaunch) {
                    // Check permissions
                    if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)
                            != PackageManager.PERMISSION_GRANTED) {
                        // Check if the user stopped permission requests
                        if(!ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                            Snackbar snackbar = Snackbar.make(layout, "Please enable file permissions in settings.", 5000);
                            snackbar.show();
                            return;
                        }
                        // Request permissions
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                                READ_PERMISSION_REQUEST);
                    }
                    else {
                        // Choose a file
                        chooseFile();
                    }
                }
                else {
                    // Move to AR Activity
                    Intent launchARIntent = new Intent(MainActivity.this, ARActivity.class);
                    launchARIntent.putExtra("Filepath", filePath);
                    startActivity(launchARIntent);
                }
            }
        });

        btnRetry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Check permissions
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    // Request permissions
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.READ_CONTACTS},
                            READ_PERMISSION_REQUEST);
                }
                else {
                    // Choose a file
                    chooseFile();
                }
            }
        });
    }

    private String getPath(Context context, Uri uri) {
        String[] proj = { MediaStore.Images.Media.DATA };
        CursorLoader loader = new CursorLoader(context, uri, proj, null, null, null);
        Cursor cursor = loader.loadInBackground();
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        String result = cursor.getString(column_index);
        cursor.close();
        return result;
    }

    private void chooseFile() {
        // Create file explorer intent for result
        Intent getFileIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        getFileIntent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);

        // Make sure that the user has something to find the file with
        if (getFileIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(getFileIntent, PICK_FILE_REQUEST);
        }
        else {
            // Tell the user that they need a media manager
            Snackbar snackbar = Snackbar.make(layout,
                    "Please ensure you have a media manager installed.", 3000);
            snackbar.show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == PICK_FILE_REQUEST) {
            if(resultCode == RESULT_OK) {

                // Get the file path from the result intent
                Uri pathURI = data.getData();

                String videoPath = getPath(this, pathURI);

                if(videoPath != null) {
                    filePath = videoPath;
                    String fileName = videoPath.substring(videoPath.lastIndexOf("/") + 1);

                    // Show success
                    textView.setText(fileName);
                    textView.setTextColor(getColor(R.color.colorPrimaryText));
                    imageView.setImageResource(R.drawable.ic_check_24dp);
                    btnChooseFile.setText("Launch");
                    btnRetry.setVisibility(View.VISIBLE);

                    // Change main button functionality
                    canLaunch = true;
                }
                else {
                    // There was an error
                    Snackbar snackbar = Snackbar.make(layout, "An error occurred. Please try again.", 3000);
                    snackbar.show();
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case READ_PERMISSION_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission was granted, proceed with file selection
                    chooseFile();
                } else {
                    // Permission was denied, tell the user why we need it
                    Snackbar snackbar = Snackbar.make(layout, "Please grant file read permissions.", 5000);
                    snackbar.show();
                }
                return;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                // Open the settings activity
                // TODO
                Toast.makeText(this, "Coming soon", Toast.LENGTH_SHORT).show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}