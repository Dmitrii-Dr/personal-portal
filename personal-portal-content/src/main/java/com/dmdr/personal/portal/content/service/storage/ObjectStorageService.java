package com.dmdr.personal.portal.content.service.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * Object storage service that works with any S3-compatible provider.
 * Supports AWS S3, Cloudflare R2, DigitalOcean Spaces, MinIO, Backblaze B2, and
 * others.
 */
@Service
@Slf4j
public class ObjectStorageService {

    private final S3Client s3Client;
    private final String bucketName;

    public ObjectStorageService(S3Client s3Client, @Value("${aws.s3.bucket-name}") String bucketName) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
    }

    /**
     * Uploads a file to object storage.
     * 
     * @param key       the object key (path/filename)
     * @param fileBytes the file content as bytes
     * @throws RuntimeException if upload fails
     */
    public void uploadFile(String key, byte[] fileBytes) {
        try {
            PutObjectRequest objectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.putObject(objectRequest, RequestBody.fromBytes(fileBytes));
            log.debug("File uploaded successfully: {}", key);
        } catch (S3Exception e) {
            String errorMessage = String.format("Object storage upload failed [%s]: %s", e.getClass().getSimpleName(),
                    e.getMessage());
            log.error(errorMessage, e);
            throw new RuntimeException("Failed to upload file to object storage", e);
        } catch (Exception e) {
            String errorMessage = String.format("Object storage upload error [%s]: %s", e.getClass().getSimpleName(),
                    e.getMessage());
            log.error(errorMessage, e);
            throw new RuntimeException("Failed to upload file to object storage", e);
        }
    }

    /**
     * Downloads a file from object storage.
     * 
     * @param key the object key (path/filename)
     * @return the file content as bytes, or null if download fails
     */
    public byte[] downloadFile(String key) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
        try {
            ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(getObjectRequest);
            log.debug("File downloaded successfully: {}", key);
            return objectBytes.asByteArray();
        } catch (S3Exception e) {
            String errorMessage = String.format("Object storage download failed [%s]: %s", e.getClass().getSimpleName(),
                    e.getMessage());
            log.error(errorMessage, e);
            return null;
        } catch (Exception e) {
            String errorMessage = String.format("Object storage download error [%s]: %s", e.getClass().getSimpleName(),
                    e.getMessage());
            log.error(errorMessage, e);
            return null;
        }
    }

    /**
     * Deletes a file from object storage.
     * 
     * @param key the object key (path/filename)
     * @throws RuntimeException if deletion fails
     */
    public void deleteFile(String key) {
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
        try {
            s3Client.deleteObject(deleteObjectRequest);
            log.debug("File deleted successfully: {}", key);
        } catch (S3Exception e) {
            String errorMessage = String.format("Object storage delete failed [%s]: %s", e.getClass().getSimpleName(),
                    e.getMessage());
            log.error(errorMessage, e);
            throw new RuntimeException("Failed to delete file from object storage", e);
        } catch (Exception e) {
            String errorMessage = String.format("Object storage delete error [%s]: %s", e.getClass().getSimpleName(),
                    e.getMessage());
            log.error(errorMessage, e);
            throw new RuntimeException("Failed to delete file from object storage", e);
        }
    }
}
