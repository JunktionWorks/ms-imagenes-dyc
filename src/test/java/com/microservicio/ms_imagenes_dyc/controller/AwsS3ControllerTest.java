package com.microservicio.ms_imagenes_dyc.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

import com.microservicio.ms_imagenes_dyc.models.dto.S3ObjectDto;
import com.microservicio.ms_imagenes_dyc.service.AwsS3Service;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AwsS3Controller.class)
class AwsS3ControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AwsS3Service awsS3Service;

    @Test
    @DisplayName("GET /s3/{bucket}/objects → lista DTOs")
    void listObjects_returnsDtoList() throws Exception {
        // Creamos dos DTOs con datos dummy
        var dto1 = new S3ObjectDto("file1.png", 100L, "2025-07-19T12:00:00");
        var dto2 = new S3ObjectDto("file2.jpg", 200L, "2025-07-20T15:30:00");

        when(awsS3Service.listObjects("bucketdyc"))
            .thenReturn(List.of(dto1, dto2));

        mockMvc.perform(get("/s3/bucketdyc/objects"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].key").value("file1.png"))
            .andExpect(jsonPath("$[0].size").value(100))
            .andExpect(jsonPath("$[0].lastModified").value("2025-07-19T12:00:00"))
            .andExpect(jsonPath("$[1].key").value("file2.jpg"))
            .andExpect(jsonPath("$[1].size").value(200))
            .andExpect(jsonPath("$[1].lastModified").value("2025-07-20T15:30:00"));

        verify(awsS3Service).listObjects("bucketdyc");
    }

    @Test
    @DisplayName("GET /s3/{bucket}/object?key=... → descarga bytes por stream")
    void getObjectAsStream_returnsBytesAndHeaders() throws Exception {
        byte[] data = "hola mundo".getBytes(StandardCharsets.UTF_8);
        when(awsS3Service.downloadAsBytes("bucketdyc", "foo.txt"))
            .thenReturn(data);

        mockMvc.perform(get("/s3/bucketdyc/object/stream")
                .param("key", "foo.txt"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", "attachment; filename=foo.txt"))
            .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
            .andExpect(content().bytes(data));

        verify(awsS3Service).downloadAsBytes("bucketdyc", "foo.txt");
    }

    @Test
    @DisplayName("POST /s3/{bucket}/object (multipart) → devuelve URL pre-firmada")
    void uploadAndGetPresignedUrl_returnsUrl() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file", "foto.png", MediaType.IMAGE_PNG_VALUE,
            "contenido".getBytes(StandardCharsets.UTF_8)
        );
        when(awsS3Service.upload("bucketdyc", "foto.png", file))
            .thenReturn("https://bucketdyc.s3.amazonaws.com/foto.png?X");

        mockMvc.perform(multipart("/s3/bucketdyc/object")
                .file(file)
                .param("key", "foto.png"))
            .andExpect(status().isOk())
            .andExpect(content().string("https://bucketdyc.s3.amazonaws.com/foto.png?X"));

        verify(awsS3Service).upload("bucketdyc", "foto.png", file);
    }

    @Test
    @DisplayName("POST /s3/{bucket}/move → mueve objeto")
    void moveObject_invokesService() throws Exception {
        mockMvc.perform(post("/s3/bucketdyc/move")
                .param("sourceKey", "a.png")
                .param("destKey", "b.png"))
            .andExpect(status().isOk());

        verify(awsS3Service).moveObject("bucketdyc", "a.png", "b.png");
    }

    @Test
    @DisplayName("DELETE /s3/{bucket}/object?key=... → borra objeto")
    void deleteObject_invokesService() throws Exception {
        mockMvc.perform(delete("/s3/bucketdyc/object")
                .param("key", "viejo.png"))
            .andExpect(status().isNoContent());

        verify(awsS3Service).deleteObject("bucketdyc", "viejo.png");
    }
}