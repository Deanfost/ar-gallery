package com.example.deanf.argallery;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifSubIFDDescriptor;
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
    private ExifSubIFDDirectory exifSubIFDDirectory;
    private ExifSubIFDDescriptor exifSubIFDDescriptor;
    private GpsDirectory gpsDirectory;
    private GpsDescriptor gpsDescriptor;
    private File image;

    public MetaParser(File image) {
        try {
            this.image = image;
            this.metadata = ImageMetadataReader.readMetadata(image);
            exifSubIFDDirectory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            exifSubIFDDescriptor = new ExifSubIFDDescriptor(exifSubIFDDirectory);
            gpsDirectory = metadata.getFirstDirectoryOfType(GpsDirectory.class);
            gpsDescriptor = new GpsDescriptor(gpsDirectory);
        } catch (ImageProcessingException | IOException e) {
            e.printStackTrace();
        }
    }

    public String getFileName() {
        // Get filename
        return image.getName();
    }

    public String getFileTime() {
        // Get the time the image was taken
        if(exifSubIFDDirectory != null) {
            Date date = exifSubIFDDirectory.getDateOriginal(TimeZone.getDefault());
            if(date != null) {
                DateFormat df = new SimpleDateFormat("MM/dd/yyyy hh:mm:aa");
                return df.format(date);
            }
            else {
                return "Date Taken - N/A";
            }
        }
        else {
            return "Date Taken - N/A";
        }
    }

    public String getFileRes() {
        // Get width x height of image
        if(exifSubIFDDescriptor != null) {
            String width = exifSubIFDDescriptor.getExifImageWidthDescription();
            String height = exifSubIFDDescriptor.getExifImageHeightDescription();
            if(width != null && height != null) {
                width = width.substring(0, width.lastIndexOf(" "));
                height = height.substring(0, height.lastIndexOf(" "));
                return width + "x" + height;
            }
            else {
                return "Resolution - N/A";
            }
        }
        else {
            return "Resolution - N/A";
        }

    }

    public String getFileSize() {
        // Get size of image in bytes then convert to mb
        Double fileSize = image.length() * B_TO_MB_RATIO;
        return (fileSize != 0) ? Double.toString(Math.round(fileSize * 100.0) / 100.0) + "MB" : "Size - N/A";
    }

    public String getTakenLocation() {
        // Get coordinates of image's location and convert from DMS to decimal
        if(gpsDescriptor != null) {
            String lat = gpsDescriptor.getGpsLatitudeDescription();
            String lon = gpsDescriptor.getGpsLongitudeDescription();
            return (lat != null && lon != null) ? lat + ", " + lon : "Location - N/A";
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