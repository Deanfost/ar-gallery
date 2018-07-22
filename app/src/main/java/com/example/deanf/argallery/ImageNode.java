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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class ImageNode extends Node implements Node.OnTapListener {
    private final int PIXEL_TO_METER_RATIO = 3000;

    private Bitmap image;
    private float imgHeight;
    public boolean isLoaded = false;
    private String filepath;

    private Node imageNode;
    private ViewRenderable imageViewRenderable;
    private Node metaDataNode;
    private ViewRenderable metaViewRenderable;
    private Context context;

    public ImageNode(String filepath, Context context) {
        this.filepath = filepath;
        this.context = context;
        setOnTapListener(this);
        this.setEnabled(false);

        // Calculate dimens of the image, set references
        image = BitmapFactory.decodeFile(filepath);
        imgHeight = image.getHeight();
    }

    public void initialize() {
        // Setup the ImageView and MetaData renderables
        imageNode = new Node();
        imageNode.setParent(this);
        imageNode.setEnabled(true);

        metaDataNode = new Node();
        metaDataNode.setEnabled(false);

        CompletableFuture<ViewRenderable> imageStage =
                ViewRenderable.builder().setView(context, R.layout.widget_image).build();
        CompletableFuture<ViewRenderable> metaStage =
                ViewRenderable.builder().setView(context, R.layout.widget_metadata).build();

        CompletableFuture.allOf(imageStage, metaStage)
                .handle((notUsed, throwable) -> {
                    if (throwable != null) {
                        throwable.printStackTrace();
                        Toast.makeText(context, "Unable to load renderable", Toast.LENGTH_SHORT).show();
                        return null;
                    }

                    try {
                        // Setup the image node
                        imageNode.setRenderable(imageStage.get());
                        imageViewRenderable = (ViewRenderable) imageNode.getRenderable();
                        imageViewRenderable.setPixelsToMetersRatio(PIXEL_TO_METER_RATIO);
                        ImageView imageView = (ImageView) imageViewRenderable.getView();
                        imageView.setImageBitmap(image);

                        // Setup the meta node
                        metaDataNode.setRenderable(metaStage.get());
                        metaViewRenderable = (ViewRenderable) metaDataNode.getRenderable();
                        TextView metaName = metaViewRenderable.getView().findViewById(R.id.meta_name);
                        TextView metaTime = metaViewRenderable.getView().findViewById(R.id.meta_time);
                        TextView metaRes = metaViewRenderable.getView().findViewById(R.id.meta_res);
                        TextView metaSize = metaViewRenderable.getView().findViewById(R.id.meta_size);
                        TextView metaLocation = metaViewRenderable.getView().findViewById(R.id.meta_location);

                        MetaParser metaParser = new MetaParser(new File(filepath));
                        metaName.setText(metaParser.getFileName());
                        metaTime.setText(metaParser.getLastModified());
                        metaRes.setText(metaParser.getFileRes());
                        metaSize.setText(metaParser.getFileSize());
                        metaLocation.setText(metaParser.getTakenLocation());

                        metaDataNode.setParent(imageNode);
                        float metersToPixelRatio = imageViewRenderable.getMetersToPixelsRatio();
                        float bottomYDistance = (imgHeight * metersToPixelRatio) / (float) 2;
                        float bottomMargin = (float) .005;
                        metaDataNode.setLocalPosition(new Vector3(0,  -bottomYDistance, 0));

                        isLoaded = true;

                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                        Toast.makeText(context, "An error occurred", Toast.LENGTH_SHORT).show();
                    }

                    return null;
                });
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

        metaDataNode.setEnabled(!metaDataNode.isEnabled());
    }
}