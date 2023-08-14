package au.com.sydneywater.imageprocessor.api.rest.service;

import au.com.sydneywater.imageprocessor.api.rest.entity.ImageRecord;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.ImageWriteException;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public interface FilesStorageService {
    byte[] process(MultipartFile file) throws ImageWriteException, IOException, ImageReadException;

    List<ImageRecord> getLastProcessedImages(String statusFilter);

    void setHistorySize(int size);

    int getHistorySize();

    Map<String, String> getAllowedTypesWithExtensions();


}
