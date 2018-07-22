package com.example.deanf.argallery;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.Toast;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDescriptor;
import com.drew.metadata.exif.GpsDirectory;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class MetaParser {
    private final Double B_TO_MB_RATIO = 0.000001;
    private Metadata metadata;
    private File image;

    public MetaParser(File image) {
        this.image = image;
        try {
            this.metadata = ImageMetadataReader.readMetadata(image);
        } catch (ImageProcessingException | IOException e) {
            e.printStackTrace();
        }
    }

    public String getFileName() {
        // Get filename
        System.out.println(image.getName());
        return image.getName();
    }

    public String getLastModified() {
        // Get time the image was last modified
        DateFormat df = new SimpleDateFormat("MM/dd/yyyy hh:mm:aa");
        Date lastModified = new Date(image.lastModified());
        System.out.println(df.format(lastModified));
        return df.format(lastModified);
    }

    public String getFileRes() {
        // Get width x height of image
        Bitmap bitmap = BitmapFactory.decodeFile(image.getPath());
        System.out.println(bitmap.getWidth() + " " + bitmap.getHeight());
        return bitmap.getWidth() + " x " + bitmap.getHeight();
    }

    public String getFileSize() {
        // Get size of image in bytes then convert to mb
        Double fileSize = image.length() * B_TO_MB_RATIO;
        System.out.println(fileSize);
        return (fileSize != 0) ? Double.toString(Math.round(fileSize * 100.0) / 100.0) + "MB" : "Size - N/A";
    }

    public String getTakenLocation() {
        // Get coordinates of image's location and convert from DMS to decimal
        if(metadata.containsDirectoryOfType(GpsDirectory.class)) {
            GpsDescriptor gpsDescriptor;
            // Loop through and attempt to find the coordinates
            for(GpsDirectory d : metadata.getDirectoriesOfType(GpsDirectory.class)) {
                gpsDescriptor = new GpsDescriptor(d);
                String lat = gpsDescriptor.getGpsLatitudeDescription();
                String lon = gpsDescriptor.getGpsLongitudeDescription();
                System.out.println(lat);
                System.out.println(lon);
                return (lat != null && lon != null) ? lat + ", " + lon : "Location - N/A";
            }
            return "Location - N/A";
        }
        else {
            return "Location - N/A";
        }
    }

    public void logAllData() {
        // Log all applicable metadata
        for(Directory dir : metadata.getDirectories()) {
            for(Tag tag : dir.getTags()) {
                System.out.println(dir + " " + tag);
            }
        }
    }
}