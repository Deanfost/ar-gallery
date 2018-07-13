package com.example.deanf.arvideo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.VideoView;

import com.google.ar.sceneform.ux.ArFragment;

public class ARActivity extends AppCompatActivity {
    private ArFragment arFragment;
    private VideoView videoView;
//    private ModelRenderable andyRenderable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ar);
        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);
        videoView = findViewById(R.id.video_view);


    }
}