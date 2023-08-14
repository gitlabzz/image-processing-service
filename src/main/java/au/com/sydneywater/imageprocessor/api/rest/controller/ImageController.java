package au.com.sydneywater.imageprocessor.api.rest.controller;

import au.com.sydneywater.imageprocessor.api.rest.exception.InvalidStatusFilterException;
import au.com.sydneywater.imageprocessor.api.rest.utils.ImageProcessingStatus;
import au.com.sydneywater.imageprocessor.api.rest.entity.ImageRecord;
import au.com.sydneywater.imageprocessor.api.rest.exception.InvalidFileTypeException;
import au.com.sydneywater.imageprocessor.api.rest.service.FilesStorageService;
import io.swagger.annotations.Api;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.ImageWriteException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/v1")
@Api(tags = {"Image Processing Service"})
public class ImageController {

    @Autowired
    FilesStorageService storageService;

    private static final Logger logger = LoggerFactory.getLogger(ImageController.class);

    @RequestMapping(path = "/image/process", method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> processImage(@RequestPart MultipartFile file) throws ImageWriteException, IOException, ImageReadException {

        // Determine the content type of the input
        String contentType = file.getContentType();
        MediaType producedMediaType = null;

        if (MediaType.IMAGE_JPEG_VALUE.equals(contentType)) {
            producedMediaType = MediaType.IMAGE_JPEG;
        } else if (MediaType.IMAGE_PNG_VALUE.equals(contentType)) {
            producedMediaType = MediaType.IMAGE_PNG;
        } else if (MediaType.APPLICATION_PDF.equals(contentType)) {
            producedMediaType = MediaType.APPLICATION_PDF;
        } else {
            logger.warn("Invalid file type: {}", contentType);
            throw new InvalidFileTypeException("Unsupported media type: Only JPEG and PNG files are supported.");
        }

        logger.info("Received image upload request for file: {}", file.getOriginalFilename());

        byte[] bytes = storageService.process(file);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(producedMediaType);
        headers.setContentLength(bytes.length);
        headers.setContentDisposition(ContentDisposition.builder("attachment").filename(file.getOriginalFilename()).build());

        logger.info("Image processed successfully: {}", file.getOriginalFilename());
        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }

    @RequestMapping(path = "/image/history", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ImageRecord>> getLastProcessedImages(@RequestParam(value = "status", required = false) ImageProcessingStatus status) {
        if (status != null && !EnumSet.allOf(ImageProcessingStatus.class).contains(status)) {
            throw new InvalidStatusFilterException("Invalid status filter: " + status);
        }

        List<ImageRecord> history = storageService.getLastProcessedImages(status == null ? null : status.name());
        logger.info("Retrieved {} image records from history.", history.size());
        return new ResponseEntity<>(history, HttpStatus.OK);
    }

    @RequestMapping(path = "/image/history/size", method = RequestMethod.POST)
    public ResponseEntity<Void> setHistorySize(@RequestParam int size) {
        storageService.setHistorySize(size);
        logger.info("Updated history size to: {}", size);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequestMapping(path = "/image/history/size", method = RequestMethod.GET)
    public ResponseEntity<Integer> getHistorySize() {
        int size = storageService.getHistorySize();
        logger.info("Retrieved history size: {}", size);
        return new ResponseEntity<>(size, HttpStatus.OK);
    }

    @GetMapping("/allowed-types-extensions")
    public ResponseEntity<Map<String, String>> getAllowedTypesWithExtensions() {
        Map<String, String> allowedTypesWithExtensions = storageService.getAllowedTypesWithExtensions();
        return ResponseEntity.ok(allowedTypesWithExtensions);
    }


}
