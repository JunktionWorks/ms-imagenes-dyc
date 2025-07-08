package com.microservicio.ms_imagenes_dyc.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.microservicio.ms_imagenes_dyc.MsImagenesDycApplication;
import com.microservicio.ms_imagenes_dyc.models.dto.S3ObjectDto;

import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
public class AwsS3Service {
    
    private final S3Client s3Client;

    private final String defaultBucket = "bucketdyc";

    public AwsS3Service(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    public List<S3ObjectDto> listObjects(String bucket) {
        ListObjectsV2Response resp = s3Client.listObjectsV2(
            ListObjectsV2Request.builder().bucket(bucket).build()
        );
        return resp.contents().stream()
                    .map(o -> new S3ObjectDto(o.key(), o.size(),
                            o.lastModified() != null ? o.lastModified().toString() : null))
                    .collect(Collectors.toList());
    }

    public byte[] downloadAsBytes(String bucket, String key) {
        ResponseBytes<GetObjectResponse> bytes = s3Client.getObjectAsBytes(
            GetObjectRequest.builder().bucket(bucket).key(key).build()
        );
        return bytes.asByteArray();
    }

    public void upload(String bucket, String key, MultipartFile file) {
        try{
            PutObjectRequest req = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();
                s3Client.putObject(req, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        } catch (Exception e) {
            throw new RuntimeException("Error uploading to S3", e);
        }
    }

    public void delete(String bucket, String key) {
        s3Client.deleteObject(
                DeleteObjectRequest.builder().bucket(bucket).key(key).build()
        );
    }

}
