package com.example.deanf.argallery;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.google.ar.core.Anchor;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.BaseArFragment;

public class ARActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final double MIN_OPENGL_VERSION = 3.1;
    // Scaling from pixels to meters (1080px -> .54m)
    private static final double IMAGE_SCALE_FACTOR = 1;

    private ArFragment arFragment;
    ImageNode imageNode;

    private FrameLayout layout;

    private String filepath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!checkIsSupportedDeviceOrFinish(this)) {
            return;
        }

        // Get references
        setContentView(R.layout.activity_ar);
        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);
        filepath = getIntent().getStringExtra("Filepath");
        layout = findViewById(R.id.ar_layout);

        // Create the first ImageNode
        imageNode = new ImageNode(filepath, IMAGE_SCALE_FACTOR, ARActivity.this);
        imageNode.initialize();

        // Setup a tap listener
        arFragment.setOnTapArPlaneListener(new BaseArFragment.OnTapArPlaneListener() {
            @Override
            public void onTapPlane(HitResult hitResult, Plane plane, MotionEvent motionEvent) {
                if(imageNode.imageLoaded && imageNode.metaLoaded) {

                    // Create an anchor
                    Anchor anchor = hitResult.createAnchor();
                    AnchorNode anchorNode = new AnchorNode(anchor);
                    anchorNode.setParent(arFragment.getArSceneView().getScene());

                    // Assign the ImageNode to the anchor, enable
                    imageNode.setParent(anchorNode);
                    imageNode.setEnabled(true);
                    imageNode.setLocalPosition(new Vector3(0, (float) .5, 0));

                    // Todo - find a new image from storage, create new node
//                    findNewFile();
//                    imageNode = createNewNode();
                }
            }
        });

        // Display a snackbar to tell the user how to get started
        Snackbar snackbar =
                Snackbar.make(layout, "Tap to place an image on a discovered surface.", 5000);
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

    private void findNewFile() {

    }

    private ImageNode createNewNode() {
        return null;
    }
}