package au.com.sydneywater.imageprocessor.api.rest.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class FileUploadExceptionAdvice {

    private static final Logger logger = LoggerFactory.getLogger(FileUploadExceptionAdvice.class);

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseBody
    public ResponseEntity<String> handleMaxSizeException(MaxUploadSizeExceededException e) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .contentType(MediaType.TEXT_PLAIN)
                .body("File is too large! The maximum allowed size is 10MB.");
    }


    @ExceptionHandler(InvalidFileTypeException.class)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> handleInvalidFileTypeException(InvalidFileTypeException e) {
        logger.warn(e.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("error", "Invalid file type");
        response.put("message", e.getMessage());
        response.put("status", HttpStatus.BAD_REQUEST.value());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(ImageProcessingException.class)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> handleImageProcessingException(ImageProcessingException e) {
        logger.error("Error processing the image", e);

        Map<String, Object> response = new HashMap<>();

        switch (e.getErrorType()) {
            case INVALID_JPEG_FORMAT:
                response.put("error", "Invalid JPEG Image Format");
                response.put("status", HttpStatus.UNSUPPORTED_MEDIA_TYPE.value()); // 415 status for validation errors
                break;
            case INVALID_PNG_FORMAT:
                response.put("error", "Invalid PNG Image Format");
                response.put("status", HttpStatus.UNSUPPORTED_MEDIA_TYPE.value()); // 415 status for validation errors
                break;
            case IMAGE_REWRITING_ERROR:
                response.put("error", "Image Rewriting Error");
                response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
                break;
            case GENERAL_ERROR:
            default:
                response.put("error", "Image processing error");
                response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
                break;
        }

        response.put("message", e.getMessage());

        return ResponseEntity.status((Integer) response.get("status")).body(response);
    }


    @ExceptionHandler(Exception.class)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> handleGeneralException(Exception e) {
        logger.error("Unexpected error", e);

        Map<String, Object> response = new HashMap<>();
        response.put("error", "Internal server error");
        response.put("message", "An unexpected error occurred.");
        response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(InvalidHistorySizeException.class)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> handleInvalidHistorySizeException(InvalidHistorySizeException e) {
        logger.warn(e.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("error", "Invalid history size");
        response.put("message", e.getMessage());
        response.put("status", HttpStatus.BAD_REQUEST.value());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(UnsupportedMediaTypeException.class)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> handleUnsupportedMediaTypeException(UnsupportedMediaTypeException e) {
        logger.error("Unsupported media type", e);

        Map<String, Object> response = new HashMap<>();
        response.put("error", "Unsupported Media Type");
        response.put("message", e.getMessage());
        response.put("status", HttpStatus.UNSUPPORTED_MEDIA_TYPE.value());

        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(response);
    }

}
