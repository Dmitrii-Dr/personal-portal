package com.dmdr.personal.portal.controller.util;

import org.springframework.web.multipart.MultipartFile;

public class ImageFileValidator {

    private ImageFileValidator() {
        // Utility class - prevent instantiation
    }

    /**
     * Validates that the uploaded file is an allowed image format.
     * SVG files are explicitly not allowed.
     * 
     * @param file The multipart file to validate
     * @return Error message if validation fails, null if validation passes
     */
    public static String validateImageFile(MultipartFile file) {
        // Validate file is not empty
        if (file.isEmpty()) {
            return "File cannot be empty.";
        }
        
        // Validate file is an allowed image format (SVG not allowed)
        String filename = file.getOriginalFilename();
        if (filename == null || filename.isEmpty()) {
            return "File must have a valid filename.";
        }
        
        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        if (extension.equals("svg")) {
            return "SVG files are not allowed.";
        }
        
        // Check if it's an allowed image format
        String contentType = file.getContentType();
        boolean isAllowedImage = false;
        
        if (contentType != null && contentType.startsWith("image/")) {
            // Check content type
            isAllowedImage = contentType.equals("image/jpeg") || 
                            contentType.equals("image/png") || 
                            contentType.equals("image/gif") || 
                            contentType.equals("image/webp");
        }
        
        // Also check by extension as fallback
        if (!isAllowedImage) {
            isAllowedImage = extension.equals("jpg") || 
                           extension.equals("jpeg") || 
                           extension.equals("png") || 
                           extension.equals("gif") || 
                           extension.equals("webp");
        }
        
        if (!isAllowedImage) {
            return "Only image files (JPEG, PNG, GIF, WebP) are allowed. SVG files are not permitted.";
        }
        
        return null; // Validation passed
    }
}

