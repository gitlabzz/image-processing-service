package au.com.sydneywater.imageprocessor.api.rest.utils;

public enum ImageProcessingStatus {
    PROCESSED_SUCCESSFULLY("Processed Successfully"),
    FAILED_TO_PROCESS("Failed to Process"),
    INVALID_JPEG_FORMAT("Invalid JPEG Format"),
    INVALID_PNG_FORMAT("Invalid PNG Format"),
    UNSUPPORTED_IMAGE_FORMAT("Unsupported Image Format"),
    IMAGE_REWRITE_ERROR("Error Rewriting Image");

    private final String status;

    ImageProcessingStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}

