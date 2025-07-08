package com.microservicio.ms_imagenes_dyc.controller;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.microservicio.ms_imagenes_dyc.models.dto.S3ObjectDto;
import com.microservicio.ms_imagenes_dyc.service.AwsS3Service;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/s3")
@RequiredArgsConstructor
public class AwsS3Controller {
    
    private final AwsS3Service s3Service;
    private final String bucket = "bucketdyc";

    @GetMapping("/{bucket}/objects")
    public ResponseEntity<List<S3ObjectDto>> list(@PathVariable String bucket) {
        return ResponseEntity.ok(s3Service.listObjects(bucket));
    }

    @PostMapping("/{bucket}/upload")
    public ResponseEntity<Void> upload(
            @PathVariable String bucket,
            @RequestParam String key,
            @RequestParam MultipartFile file) {
        s3Service.upload(bucket, key, file);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{bucket}/download")
    public ResponseEntity<byte[]> download(
            @PathVariable String bucket,
            @RequestParam String key) {
        byte[] data = s3Service.downloadAsBytes(bucket, key);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + key + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }

    @DeleteMapping("/{bucket}/object")
    public ResponseEntity<Void> delete(
            @PathVariable String bucket,
            @RequestParam String key) {
        s3Service.delete(bucket, key);
        return ResponseEntity.noContent().build();
    }

}
