package com.dmdr.personal.portal.content.service;

import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
@Slf4j
public class ThumbnailService {

    private static final int MAX_THUMBNAIL_WIDTH = 200;

    /**
     * Generates a thumbnail from image bytes.
     * Creates a thumbnail with max width of 200px, preserving aspect ratio.
     * 
     * @param imageBytes The original image bytes
     * @return The thumbnail bytes, or null if generation fails
     */
    public byte[] generateThumbnail(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length == 0) {
            log.warn("Cannot generate thumbnail: image bytes are null or empty");
            return null;
        }

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            
            Thumbnails.of(new ByteArrayInputStream(imageBytes))
                    .width(MAX_THUMBNAIL_WIDTH)
                    .keepAspectRatio(true)
                    .outputFormat("jpg") // Always output as JPEG for consistency
                    .toOutputStream(outputStream);
            
            return outputStream.toByteArray();
        } catch (IOException e) {
            log.error("Failed to generate thumbnail", e);
            return null;
        }
    }

    /**
     * Checks if the given file type or filename indicates an SVG image.
     * SVG files should not have thumbnails generated.
     * 
     * @param fileType The MIME type (e.g., "image/svg+xml")
     * @param filename The original filename (may be null)
     * @return true if the file is an SVG, false otherwise
     */
    public boolean isSvg(String fileType, String filename) {
        if (fileType != null && fileType.contains("svg")) {
            return true;
        }
        if (filename != null && filename.toLowerCase().endsWith(".svg")) {
            return true;
        }
        return false;
    }
}
