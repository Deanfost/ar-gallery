package com.example.deanf.arvideo;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.ResultReceiver;
import android.support.design.widget.Snackbar;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.VideoView;

import com.google.ar.core.Anchor;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.BaseArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

public class ARActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final double MIN_OPENGL_VERSION = 3.1;
    private static final String LOG_TAG = "MediaSession";


    private ArFragment arFragment;
    private ViewRenderable screenRenderable;
    private ArSceneView arSceneView;

    MediaSessionCompat mediaSession;
    PlaybackStateCompat.Builder stateBuilder;

    private FrameLayout layout;
    private VideoView videoView;
    private ImageButton btnBack10;
    private ImageButton btnToggle;
    private ImageButton btnFoward30;

    private String filepath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!checkIsSupportedDeviceOrFinish(this)) {
            return;
        }

        setContentView(R.layout.activity_ar);
        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);

        // Setup Media Player components
        mediaSession = new MediaSessionCompat(this, LOG_TAG);
        mediaSession.setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mediaSession.setMediaButtonReceiver(null);
        stateBuilder = new PlaybackStateCompat.Builder()
                .setActions(
                        PlaybackStateCompat.ACTION_PLAY |
                                PlaybackStateCompat.ACTION_PLAY_PAUSE |
                                PlaybackStateCompat.ACTION_SEEK_TO);
        mediaSession.setPlaybackState(stateBuilder.build());
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onCommand(String command, Bundle extras, ResultReceiver cb) {
                super.onCommand(command, extras, cb);
            }
        });

        


        // Create the renderable
        ViewRenderable.builder()
                .setView(this, R.layout.widget_video)
                .build()
                .thenAccept(renderable -> this.screenRenderable = renderable);

        // Get references
        filepath = getIntent().getStringExtra("Filepath");
        layout = findViewById(R.id.ar_layout);
        videoView =  screenRenderable.getView().findViewById(R.id.video_view);
        btnBack10 = screenRenderable.getView().findViewById(R.id.btn_back);
        btnToggle = screenRenderable.getView().findViewById(R.id.btn_toggle);
        btnFoward30 = screenRenderable.getView().findViewById(R.id.btn_forward);

        // Setup a tap listener
        arFragment.setOnTapArPlaneListener(new BaseArFragment.OnTapArPlaneListener() {
            @Override
            public void onTapPlane(HitResult hitResult, Plane plane, MotionEvent motionEvent) {
                // Create an anchor
                Anchor anchor = hitResult.createAnchor();
                AnchorNode anchorNode = new AnchorNode(anchor);
                anchorNode.setParent(arFragment.getArSceneView().getScene());

                // Create a transformable screen and add to the anchor
                TransformableNode screen = new TransformableNode(arFragment.getTransformationSystem());
                screen.setParent(anchorNode);
                screen.setRenderable(screenRenderable);
                screen.select();

                // Play the video on the renderable

            }
        });

        // Display a snackbar to tell the user how to get started
        Snackbar snackbar = Snackbar.make(layout, "Tap to place a display on a discovered surface.", 5000);
        snackbar.show();
    }

    public static boolean checkIsSupportedDeviceOrFinish(final Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Log.e(TAG, "Sceneform requires Android N or later");
            Toast.makeText(activity, "Sceneform requires Android N or later", Toast.LENGTH_LONG).show();
            activity.finish();
            return false;
        }
        String openGlVersionString =
                ((ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE))
                        .getDeviceConfigurationInfo()
                        .getGlEsVersion();
        if (Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
            Log.e(TAG, "Sceneform requires OpenGL ES 3.1 later");
            Toast.makeText(activity, "Sceneform requires OpenGL ES 3.1 or later", Toast.LENGTH_LONG)
                    .show();
            activity.finish();
            return false;
        }
        return true;
    }
}