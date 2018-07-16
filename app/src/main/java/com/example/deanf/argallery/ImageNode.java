package com.example.deanf.argallery;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.ar.sceneform.HitTestResult;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ViewRenderable;

import java.io.File;

public class ImageNode extends Node implements Node.OnTapListener {
    private Bitmap image;
    private float imgWidth, imgHeight;
    private double imageScale;
    public boolean imageLoaded, metaLoaded = false;

    private Node imageNode;
    ViewRenderable imageViewRenderable;
    private Node metaDataNode;
    ViewRenderable metaViewRenderable;
    private final Context context;

    public ImageNode(String filePath, double imageScale, Context context) {
        // Calculate dimens of the image, set references
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        image = BitmapFactory.decodeFile(new File(filePath).getAbsolutePath(), options);
        imgWidth = options.outWidth;
        imgHeight = options.outHeight;

        this.context = context;
        this.imageScale = imageScale;
        setOnTapListener(this);
        this.setEnabled(false);
    }

    public void initialize() {
        // Setup the ImageView and MetaData renderables
            imageNode = new Node();
            imageNode.setParent(this);
            imageNode.setEnabled(true);

        ViewRenderable.builder()
                .setView(context, R.layout.widget_image)
                .build()
                .thenAccept(viewRenderable -> {
                    imageNode.setRenderable(viewRenderable);
                    imageViewRenderable = viewRenderable;

                    // Set the image source
                    ImageView imageView = (ImageView) viewRenderable.getView();
//                        imageView.setImageBitmap(image);

                    // todo - scale down?

                    // Done loading
                    imageLoaded = true;

                    // Attempt to position the meta node
                    attemptPositionMetaNode();
                })
                .exceptionally((throwable -> {
                    throw new AssertionError("Could not load image view.", throwable);
                }));

        metaDataNode = new Node();
        metaDataNode.setEnabled(false);

        ViewRenderable.builder()
                .setView(context, R.layout.widget_metadata)
                .build()
                .thenAccept(viewRenderable -> {
                    metaDataNode.setRenderable(viewRenderable);
                    metaViewRenderable = viewRenderable;

                    // Set metadata info

                    // Done loading
                    metaLoaded = true;

                    // Attempt to position the meta node
                    attemptPositionMetaNode();
                })
        .exceptionally(throwable -> {
            throw new AssertionError("Could not load meta view.", throwable);
        });
    }

    private void attemptPositionMetaNode() {
        // This method is called twice (in both .thenAccept() calls) to offset the
        // uncertainty of which thread completes first.
        if(imageLoaded && metaLoaded) {
            // Locally position the metadata renderable
            metaDataNode.setParent(imageNode);
            float metersToPixelRatio = imageViewRenderable.getMetersToPixelsRatio();
            float rightSideX = (imgWidth * metersToPixelRatio) / 2;
            float midePointY = (imgHeight * metersToPixelRatio) / 2;
            float zOffset = (float) .05;
            metaDataNode.setLocalPosition(new Vector3(62 * metersToPixelRatio, 62 * metersToPixelRatio, zOffset));
        }
    }

    @Override
    public void onActivate() {
        if (getScene() == null) {
            throw new IllegalStateException("Scene is null!");
        }
    }

    @Override
    public void onTap(HitTestResult hitTestResult, MotionEvent motionEvent) {
        if (metaDataNode == null) {
            return;
        }

        Toast.makeText(context, "You did a tapping", Toast.LENGTH_SHORT).show();

        metaDataNode.setEnabled(!metaDataNode.isEnabled());
    }
}