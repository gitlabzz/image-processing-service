package au.com.sydneywater.imageprocessor.api.rest.exception;

public class ImageProcessingException extends RuntimeException {

    public enum ErrorType {
        INVALID_JPEG_FORMAT,       // This indicates the image isn't a valid JPEG
        INVALID_PNG_FORMAT,       // This indicates the image isn't a valid PNG
        UNSUPPORTED_IMAGE_FORMAT, // This indicates that the image type is not supported (neither JPEG nor PNG, for instance)
        IMAGE_REWRITING_ERROR,    // For errors that occur during the rewriting of an image
        GENERAL_ERROR;            // For other general image processing errors
    }

    private final ErrorType errorType;

    public ImageProcessingException(String message, ErrorType errorType) {
        super(message);
        this.errorType = errorType;
    }

    public ImageProcessingException(String message, Throwable cause, ErrorType errorType) {
        super(message, cause);
        this.errorType = errorType;
    }

    public ErrorType getErrorType() {
        return errorType;
    }
}

