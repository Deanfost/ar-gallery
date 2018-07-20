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
import com.google.ar.sceneform.ux.TransformableNode;
import com.google.ar.sceneform.ux.TransformationSystem;

import java.io.File;

public class ImageNode extends TransformableNode implements Node.OnTapListener {
    private Bitmap image;
    private float imgWidth, imgHeight;
    public boolean imageLoaded, metaLoaded = false;
    private String filepath;

    private Node imageNode;
    private ViewRenderable imageViewRenderable;
    private Node metaDataNode;
    private ViewRenderable metaViewRenderable;
    private Context context;

    public ImageNode(TransformationSystem transformationSystem) {
        super(transformationSystem);
    }

    public void initialize(String filepath, Context context) {
        this.filepath = filepath;
        this.context = context;
        setOnTapListener(this);
        this.setEnabled(false);

        // Calculate dimens of the image, set references
        image = BitmapFactory.decodeFile(filepath);
        imgWidth = image.getWidth();
        imgHeight = image.getHeight();

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
                    System.out.println(throwable.getMessage());
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
//                    metaName.setText(metaParser.getFileName());
//                    metaTime.setText(metaParser.getFileTime());
//                    metaRes.setText(metaParser.getFileRes());
//                    metaSize.setText(metaParser.getFileSize());
//                    metaLocation.setText(metaParser.getTakenLocation());

                    // Done loading
                    metaLoaded = true;

                    // Attempt to position the meta node
                    attemptPositionMetaNode();
                })
        .exceptionally(throwable -> {
            System.out.println("Throwable - " + throwable.getMessage());
            throw new AssertionError("Could not load meta view.", throwable);
        });
    }

    private void attemptPositionMetaNode() {
        if(imageLoaded && metaLoaded) {
            // Locally position the metadata renderable
            metaDataNode.setParent(this);
            System.out.println("Img width - " + imgWidth);
            System.out.println("Img height - " + imgHeight);
            float metersToPixelRatio = imageViewRenderable.getMetersToPixelsRatio();
            System.out.println("MtPR - " + metersToPixelRatio);
            float rightSideX = (imgWidth * metersToPixelRatio) / (float) 6.5;
            System.out.println("Right side - " + rightSideX);
            metaDataNode.setLocalPosition(new Vector3(rightSideX,  0, 0));
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
        if (!metaLoaded) {
            Toast.makeText(context, "No metadata available", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(context, "Spawning", Toast.LENGTH_SHORT).show();
        metaDataNode.setEnabled(!metaDataNode.isEnabled());
    }
}