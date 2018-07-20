package com.example.deanf.argallery;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ar.sceneform.HitTestResult;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ViewRenderable;

import java.io.File;

public class ImageNode extends Node implements Node.OnTapListener {
    private Bitmap image;
    private float imgWidth, imgHeight;
//    private double imageScale;
    public boolean imageLoaded, metaLoaded = false;
    private String filepath;

    private Node imageNode;
    ViewRenderable imageViewRenderable;
    private Node metaDataNode;
    ViewRenderable metaViewRenderable;
    private final Context context;

    public ImageNode(String filePath, double imageScale, Context context) {
        // Calculate dimens of the image, set references
        image = BitmapFactory.decodeFile(filePath);
        imgWidth = image.getWidth();
        imgHeight = image.getHeight();

        this.filepath = filePath;
        this.context = context;
//        this.imageScale = imageScale;
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
                    imageViewRenderable = (ViewRenderable) imageNode.getRenderable();

                    // Set the image source
                    ImageView imageView = (ImageView) imageViewRenderable.getView();
                    imageView.setImageBitmap(image);

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

                    // Get metadata view references
                    TextView metaName = metaViewRenderable.getView().findViewById(R.id.meta_name);
                    TextView metaTime = metaViewRenderable.getView().findViewById(R.id.meta_time);
                    TextView metaRes = metaViewRenderable.getView().findViewById(R.id.meta_res);
                    TextView metaSize = metaViewRenderable.getView().findViewById(R.id.meta_size);
                    TextView metaLocation = metaViewRenderable.getView().findViewById(R.id.meta_location);

                    // Get metadata of images
                    MetaParser metaParser = new MetaParser(new File(filepath));

                    // Assign data
                    metaName.setText(metaParser.getFileName());
                    metaTime.setText(metaParser.getFileTime());
                    metaRes.setText(metaParser.getFileRes());
                    metaSize.setText(metaParser.getFileSize());
                    metaLocation.setText(metaParser.getTakenLocation());

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
        if(imageLoaded && metaLoaded) {
            // Locally position the metadata renderable
            metaDataNode.setParent(imageNode);
            float metersToPixelRatio = imageViewRenderable.getMetersToPixelsRatio();
            float rightSideX = (imgWidth * metersToPixelRatio) / 2;
            float midPointY = (imgHeight * metersToPixelRatio) / 2;
            metaDataNode.setLocalPosition(new Vector3(rightSideX,  midPointY, 0));
            Toast.makeText(context, "right side " + rightSideX, Toast.LENGTH_SHORT).show();
            Toast.makeText(context, "midpoint " + midPointY, Toast.LENGTH_SHORT).show();
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