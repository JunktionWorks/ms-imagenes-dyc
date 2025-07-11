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
@RequestMapping("/s3")
@RequiredArgsConstructor
public class AwsS3Controller {

	private final AwsS3Service awsS3Service;

	// Listar objetos en un bucket
	@GetMapping("/{bucket}/objects")
	public ResponseEntity<List<S3ObjectDto>> listObjects(@PathVariable String bucket) {

		List<S3ObjectDto> dtoList = awsS3Service.listObjects(bucket);
		return ResponseEntity.ok(dtoList);
	}

	// Obtener objeto como stream
	@GetMapping("/{bucket}/object/stream")
	public ResponseEntity<byte[]> getObjectAsStream(@PathVariable String bucket, @RequestParam String key) {
		byte[] fileBytes = awsS3Service.downloadAsBytes(bucket, key);
		return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + key)
				.contentType(MediaType.APPLICATION_OCTET_STREAM).body(fileBytes);
	}

	// Descargar archivo como byte[]
	@GetMapping("/{bucket}/object")
	public ResponseEntity<byte[]> downloadObject(@PathVariable String bucket, @RequestParam String key) {
		byte[] fileBytes = awsS3Service.downloadAsBytes(bucket, key);
		return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + key)
				.contentType(MediaType.APPLICATION_OCTET_STREAM).body(fileBytes);
	}

	// Subir archivo
	@PostMapping("/{bucket}/object")
	public ResponseEntity<Void> uploadObject(@PathVariable String bucket, @RequestParam String key,
			@RequestParam("file") MultipartFile file) {

		awsS3Service.upload(bucket, key, file);
		return ResponseEntity.ok().build();
	}

	// Mover objeto dentro del mismo bucket
	@PostMapping("/{bucket}/move")
	public ResponseEntity<Void> moveObject(@PathVariable String bucket, @RequestParam String sourceKey,
			@RequestParam String destKey) {
		awsS3Service.moveObject(bucket, sourceKey, destKey);
		return ResponseEntity.ok().build();
	}

	// Borrar objeto por key
	@DeleteMapping("/{bucket}/object")
	public ResponseEntity<Void> deleteObject(@PathVariable String bucket, @RequestParam String key) {
		awsS3Service.deleteObject(bucket, key);
		return ResponseEntity.noContent().build();
	}
}