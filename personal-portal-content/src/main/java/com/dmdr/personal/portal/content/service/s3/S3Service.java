package com.dmdr.personal.portal.content.service.s3;

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

@Service
@Slf4j
public class S3Service {

    private final S3Client s3Client;
    private final String bucketName;

    public S3Service(S3Client s3Client, @Value("${aws.s3.bucket-name}") String bucketName) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
    }

    public void uploadFile(String key, byte[] fileBytes) {
        try {
            PutObjectRequest objectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();
            s3Client.putObject(objectRequest, RequestBody.fromBytes(fileBytes));
        } catch (S3Exception e) {
            String errorMessage = String.format("S3 upload failed [%s]: %s", e.getClass().getSimpleName(), e.getMessage());
            log.error(errorMessage, e);
            throw new RuntimeException("Failed to upload file to S3", e);
        } catch (Exception e) {
            String errorMessage = String.format("S3 upload error [%s]: %s", e.getClass().getSimpleName(), e.getMessage());
            log.error(errorMessage, e);
            throw new RuntimeException("Failed to upload file to S3", e);
        }
    }

    public byte[] downloadFile(String key) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
        try {
            ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(getObjectRequest);
            return objectBytes.asByteArray();
        } catch (S3Exception e) {
            String errorMessage = String.format("S3 download failed [%s]: %s", e.getClass().getSimpleName(), e.getMessage());
            log.error(errorMessage, e);
            return null;
        } catch (Exception e) {
            String errorMessage = String.format("S3 download error [%s]: %s", e.getClass().getSimpleName(), e.getMessage());
            log.error(errorMessage, e);
            return null;
        }
    }

    public void deleteFile(String key) {
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
        try {
            s3Client.deleteObject(deleteObjectRequest);
        } catch (S3Exception e) {
            String errorMessage = String.format("S3 delete failed [%s]: %s", e.getClass().getSimpleName(), e.getMessage());
            log.error(errorMessage, e);
            throw new RuntimeException("Failed to delete file from S3", e);
        } catch (Exception e) {
            String errorMessage = String.format("S3 delete error [%s]: %s", e.getClass().getSimpleName(), e.getMessage());
            log.error(errorMessage, e);
            throw new RuntimeException("Failed to delete file from S3", e);
        }
    }
}
