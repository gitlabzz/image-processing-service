package au.com.sydneywater.imageprocessor.api.rest.entity;

import au.com.sydneywater.imageprocessor.api.rest.utils.ImageProcessingStatus;

import java.util.Date;

public class ImageRecord {
    private String fileName;
    private ImageProcessingStatus status;
    private Date timestamp;

    public ImageRecord(String fileName, ImageProcessingStatus status, Date timestamp) {
        this.fileName = fileName;
        this.status = status;
        this.timestamp = timestamp;
    }

    public String getFileName() {
        return fileName;
    }

    public ImageProcessingStatus getStatus() {
        return status;
    }

    public String getStatusString() {
        return status.getStatus();
    }

    public Date getTimestamp() {
        return timestamp;
    }
}
