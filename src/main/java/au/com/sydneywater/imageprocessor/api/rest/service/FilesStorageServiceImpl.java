package au.com.sydneywater.imageprocessor.api.rest.service;

import au.com.sydneywater.imageprocessor.api.rest.entity.ImageRecord;
import au.com.sydneywater.imageprocessor.api.rest.exception.ImageProcessingException;
import au.com.sydneywater.imageprocessor.api.rest.exception.InvalidHistorySizeException;
import au.com.sydneywater.imageprocessor.api.rest.exception.InvalidStatusFilterException;
import au.com.sydneywater.imageprocessor.api.rest.exception.UnsupportedMediaTypeException;
import au.com.sydneywater.imageprocessor.api.rest.utils.ImageProcessingStatus;
import org.apache.commons.imaging.*;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FilesStorageServiceImpl implements FilesStorageService {

    private static final Logger logger = LoggerFactory.getLogger(FilesStorageServiceImpl.class);

    @Value("${imageProcessing.historySize:10}")
    private int historySize;

    private LinkedList<ImageRecord> imageHistory = new LinkedList<>();

    // Define allowed content types and corresponding extensions
    private static final Map<String, String> allowedTypesWithExtensions = new HashMap<>() {{
        put("image/jpeg", "jpg");
        put("image/png", "png");
        put("application/pdf", "pdf");
    }};


    @Override
    public byte[] process(MultipartFile file) throws ImageWriteException, IOException, ImageReadException {
        String fileName = file.getOriginalFilename();
        logger.debug("Processing file: {}", fileName);

        if (file.isEmpty()) {
            logger.error("Received an empty or null file: {}", file.getOriginalFilename());
            throw new ImageProcessingException("File cannot be empty or null.", ImageProcessingException.ErrorType.GENERAL_ERROR);
        }

        String contentType = file.getContentType();
        String fileExtension = fileName.substring(fileName.lastIndexOf(".") + 1);

        // Check file's content type
        if (!allowedTypesWithExtensions.containsKey(contentType)) {
            logger.error("Unsupported media type: {}", contentType);
            throw new UnsupportedMediaTypeException("Unsupported media type: " + contentType);
        }

        // Check file's extension matches content type
        if (!fileExtension.equals(allowedTypesWithExtensions.get(contentType))) {
            logger.error("File extension does not match media type.");
            throw new UnsupportedMediaTypeException("File extension does not match media type.");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            logger.error("Failed to read the image: {}", file.getOriginalFilename(), e);
            throw new ImageProcessingException("Failed to read the image: " + e.getMessage(), e, ImageProcessingException.ErrorType.GENERAL_ERROR);
        }

        // Validate that the image is a true JPEG if its content type indicates it's a JPEG
        if (MediaType.IMAGE_JPEG_VALUE.equals(contentType) && !isTrueJPEG(bytes)) {
            logger.error("The image does not seem to be a valid JPEG: {}", file.getOriginalFilename());
            throw new ImageProcessingException("The image does not seem to be a valid JPEG.", ImageProcessingException.ErrorType.INVALID_JPEG_FORMAT);
        }

        // Validate that the image is a true PNG if its content type indicates it's a PNG
        if (MediaType.IMAGE_PNG_VALUE.equals(contentType) && !isTruePNG(bytes)) {
            logger.error("The image does not seem to be a valid PNG: {}", file.getOriginalFilename());
            throw new ImageProcessingException("The image does not seem to be a valid PNG.", ImageProcessingException.ErrorType.INVALID_PNG_FORMAT);
        }

        BufferedImage bufferedImage;
        try {
            bufferedImage = ImageIO.read(new ByteArrayInputStream(bytes));
            if (bufferedImage == null) {
                logger.error("Unsupported or corrupted image format: {}", file.getOriginalFilename());
                throw new ImageProcessingException("Unsupported or corrupted image format.", ImageProcessingException.ErrorType.UNSUPPORTED_IMAGE_FORMAT);
            }
        } catch (IOException e) {
            logger.error("Failed to decode the image: {}", file.getOriginalFilename(), e);
            throw new ImageProcessingException("Failed to decode the image: " + e.getMessage(), e, ImageProcessingException.ErrorType.GENERAL_ERROR);
        }

        ImageProcessingStatus processingStatus;
        try {
            if (MediaType.IMAGE_JPEG_VALUE.equals(contentType)) {
                bytes = processJPEG(bytes);
            } else if (MediaType.IMAGE_PNG_VALUE.equals(contentType)) {
                bytes = rewriteImage(bufferedImage, "png");
            } else if ("application/pdf".equals(contentType)) {
                logger.info("Received a PDF file: {}. No modifications will be made.", file.getOriginalFilename());
                processingStatus = ImageProcessingStatus.PROCESSED_SUCCESSFULLY;
            } else {
                logger.error("Unsupported image type: {}", contentType);
                throw new ImageProcessingException("Unsupported image type: " + contentType, ImageProcessingException.ErrorType.UNSUPPORTED_IMAGE_FORMAT);
            }

            if (bytes != null && bytes.length > 0) {
                processingStatus = ImageProcessingStatus.PROCESSED_SUCCESSFULLY;
            } else {
                processingStatus = ImageProcessingStatus.FAILED_TO_PROCESS;
            }
        } catch (Exception e) {
            processingStatus = ImageProcessingStatus.FAILED_TO_PROCESS;
            throw e;
        }

        updateImageHistory(new ImageRecord(fileName, processingStatus, new Date()));
        logger.debug("File processing completed: {}", file.getOriginalFilename());
        return bytes;
    }


    private boolean isTrueJPEG(byte[] bytes) {
        // First, we use the byte array to get an initial guess on the format
        ImageFormat imageFormat;
        try {
            imageFormat = Imaging.guessFormat(bytes);
        } catch (Exception e) {
            logger.warn("Failed to guess image format for the provided bytes", e);
            return false;
        }

        // If the initial guess isn't JPEG, we can return early without needing to do further parsing
        if (imageFormat != ImageFormats.JPEG) {
            return false;
        }

        // If the initial guess is JPEG, we further validate by attempting to parse the image
        try (ByteArrayInputStream is = new ByteArrayInputStream(bytes)) {
            Imaging.getBufferedImage(is);
            return true;  // Successfully parsed as JPEG
        } catch (ImageReadException e) {
            logger.warn("The image data could not be correctly read as a JPEG", e);
        } catch (IOException e) {
            logger.error("An I/O exception occurred while validating the image", e);
        }

        return false;
    }

    private boolean isTruePNG(byte[] bytes) {
        // First, we use the byte array to get an initial guess on the format
        ImageFormat imageFormat;
        try {
            imageFormat = Imaging.guessFormat(bytes);
        } catch (Exception e) {
            logger.warn("Failed to guess image format for the provided bytes", e);
            return false;
        }

        // If the initial guess isn't PNG, we can return early without needing to do further parsing
        if (imageFormat != ImageFormats.PNG) {
            return false;
        }

        // If the initial guess is PNG, we further validate by attempting to parse the image
        try (ByteArrayInputStream is = new ByteArrayInputStream(bytes)) {
            Imaging.getBufferedImage(is);
            return true;  // Successfully parsed as PNG
        } catch (ImageReadException e) {
            logger.warn("The image data could not be correctly read as a PNG", e);
        } catch (IOException e) {
            logger.error("An I/O exception occurred while validating the image", e);
        }

        return false;
    }


    private byte[] processJPEG(byte[] bytes) throws ImageReadException, ImageWriteException, IOException {
        try (InputStream inputStream = new ByteArrayInputStream(bytes)) {
            ImageMetadata metadata = Imaging.getMetadata(inputStream, null);
            if (metadata instanceof JpegImageMetadata) {
                JpegImageMetadata jpegMetadata = (JpegImageMetadata) metadata;
                TiffImageMetadata exif = jpegMetadata.getExif();

                if (exif != null) {
                    try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
                        new ExifRewriter().removeExifMetadata(bytes, os);
                        bytes = os.toByteArray();  // Updating the bytes directly in this method
                    }
                }
            }
        }
        return bytes; // Return updated bytes
    }

    private void updateImageHistory(ImageRecord record) {
        if (imageHistory.size() >= historySize) {
            imageHistory.poll(); // Removes the oldest element
        }
        imageHistory.add(record);
    }

    @Override
    public List<ImageRecord> getLastProcessedImages(String statusFilter) {
        if (statusFilter == null || statusFilter.isEmpty()) {
            return new ArrayList<>(imageHistory);
        }

        ImageProcessingStatus filterStatus;
        try {
            filterStatus = ImageProcessingStatus.valueOf(statusFilter.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidStatusFilterException("Invalid status filter provided: " + statusFilter);
        }

        return imageHistory.stream()
                .filter(record -> filterStatus.equals(record.getStatus()))
                .collect(Collectors.toList());
    }

    @Override
    public void setHistorySize(int size) {
        if (size <= 0) {
            throw new InvalidHistorySizeException("History size must be greater than zero");
        }
        this.historySize = size;

        // Trim history to fit the new size if necessary
        while (imageHistory.size() > historySize) {
            imageHistory.poll();
        }
    }

    @Override
    public int getHistorySize() {
        return this.historySize;
    }

    @Override
    public Map<String, String> getAllowedTypesWithExtensions() {
        return allowedTypesWithExtensions;
    }

    private byte[] rewriteImage(BufferedImage bufferedImage, String formatName) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(bufferedImage, formatName, baos);
            return baos.toByteArray();
        } catch (IOException e) {
            logger.error("Failed to rewrite the {} image.", formatName, e);
            throw new ImageProcessingException("Failed to rewrite the " + formatName + " image: " + e.getMessage(), e, ImageProcessingException.ErrorType.IMAGE_REWRITING_ERROR);
        }
    }


}
