package com.microservicio.ms_imagenes_dyc.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import com.microservicio.ms_imagenes_dyc.models.dto.S3ObjectDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

@ExtendWith(MockitoExtension.class)
class AwsS3ServiceTest {

    @Mock
    private S3Client s3Client;

    @InjectMocks
    private AwsS3Service service;

    @Test
    @DisplayName("listObjects → convierte S3Object a S3ObjectDto")
    void listObjects_returnsDtoList() {
        // Preparar objetos simulados
        S3Object obj1 = S3Object.builder()
                .key("a.txt")
                .size(123L)
                .lastModified(Instant.parse("2025-07-19T10:15:30Z"))
                .build();
        S3Object obj2 = S3Object.builder()
                .key("b.jpg")
                .size(456L)
                .lastModified(Instant.parse("2025-07-20T12:00:00Z"))
                .build();

        ListObjectsV2Response resp = ListObjectsV2Response.builder()
                .contents(obj1, obj2)
                .build();

        // Stubbing del método manual
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class)))
                .thenReturn(resp);

        // Llamada al servicio
        List<S3ObjectDto> dtos = service.listObjects("bucketdyc");

        // Aserciones
        assertThat(dtos).hasSize(2);
        assertThat(dtos.get(0).getKey()).isEqualTo("a.txt");
        assertThat(dtos.get(0).getSize()).isEqualTo(123L);
        assertThat(dtos.get(0).getLastModified()).isEqualTo("2025-07-19T10:15:30Z");
        assertThat(dtos.get(1).getKey()).isEqualTo("b.jpg");
        assertThat(dtos.get(1).getSize()).isEqualTo(456L);
        assertThat(dtos.get(1).getLastModified()).isEqualTo("2025-07-20T12:00:00Z");

        // Capturar argumento
        ArgumentCaptor<ListObjectsV2Request> listCaptor =
                ArgumentCaptor.forClass(ListObjectsV2Request.class);
        verify(s3Client).listObjectsV2(listCaptor.capture());
        assertThat(listCaptor.getValue().bucket()).isEqualTo("bucketdyc");
    }

    @Test
    @DisplayName("downloadAsBytes → retorna el array de bytes del objeto")
    void downloadAsBytes_returnsBytes() {
        byte[] content = "hola".getBytes(StandardCharsets.UTF_8);
        ResponseBytes<GetObjectResponse> mockBytes =
                ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), content);

        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenReturn(mockBytes);

        byte[] result = service.downloadAsBytes("bucketdyc", "clave.txt");
        assertThat(result).isEqualTo(content);

        // Capturar petición
        ArgumentCaptor<GetObjectRequest> getCaptor =
                ArgumentCaptor.forClass(GetObjectRequest.class);
        verify(s3Client).getObjectAsBytes(getCaptor.capture());
        assertThat(getCaptor.getValue().bucket()).isEqualTo("bucketdyc");
        assertThat(getCaptor.getValue().key()).isEqualTo("clave.txt");
    }

    @Test
    @DisplayName("upload → invoca putObject y devuelve URL pública")
    void upload_callsPutAndReturnsUrl() throws Exception {
        byte[] data = "datos".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile(
                "file", "mi.txt", "text/plain", data
        );

        String url = service.upload("bucketdyc", "mi espacio.txt", file);
        assertThat(url).isEqualTo("https://bucketdyc.s3.amazonaws.com/mi+espacio.txt");

        ArgumentCaptor<PutObjectRequest> putCaptor =
                ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(putCaptor.capture(), any(RequestBody.class));
        PutObjectRequest req = putCaptor.getValue();
        assertThat(req.bucket()).isEqualTo("bucketdyc");
        assertThat(req.key()).isEqualTo("mi espacio.txt");
        assertThat(req.contentLength()).isEqualTo(data.length);
    }

    @Test
    @DisplayName("moveObject → copia y luego borra el origen")
    void moveObject_copiesThenDeletes() {
        service.moveObject("bucketdyc", "origen.txt", "destino.txt");
        ArgumentCaptor<CopyObjectRequest> copyCaptor =
                ArgumentCaptor.forClass(CopyObjectRequest.class);
        verify(s3Client).copyObject(copyCaptor.capture());
        assertThat(copyCaptor.getValue().sourceBucket()).isEqualTo("bucketdyc");
        assertThat(copyCaptor.getValue().sourceKey()).isEqualTo("origen.txt");
        assertThat(copyCaptor.getValue().destinationKey()).isEqualTo("destino.txt");

        ArgumentCaptor<DeleteObjectRequest> deleteCaptor =
                ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(deleteCaptor.capture());
        assertThat(deleteCaptor.getValue().bucket()).isEqualTo("bucketdyc");
        assertThat(deleteCaptor.getValue().key()).isEqualTo("origen.txt");
    }

    @Test
    @DisplayName("deleteObject → llama a deleteObject del cliente S3")
    void deleteObject_invokesDelete() {
        service.deleteObject("bucketdyc", "archivo.dat");
        ArgumentCaptor<DeleteObjectRequest> deleteCaptor =
                ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(deleteCaptor.capture());
        assertThat(deleteCaptor.getValue().bucket()).isEqualTo("bucketdyc");
        assertThat(deleteCaptor.getValue().key()).isEqualTo("archivo.dat");
    }
}
