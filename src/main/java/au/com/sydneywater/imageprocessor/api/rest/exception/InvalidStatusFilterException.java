package au.com.sydneywater.imageprocessor.api.rest.exception;

public class InvalidStatusFilterException extends RuntimeException {

    public InvalidStatusFilterException(String message) {
        super(message);
    }

    public InvalidStatusFilterException(String message, Throwable cause) {
        super(message, cause);
    }
}
