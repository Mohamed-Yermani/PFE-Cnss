package com.projet.cnss.services;

import io.minio.*;
import io.minio.http.Method;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MinioServiceTest {

    @Mock
    private MinioClient minioClient;

    @InjectMocks
    private MinioService minioService;

    private MultipartFile file;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(minioService, "bucketName", "test-bucket");
        file = new MockMultipartFile("file", "document.pdf",
                "application/pdf", "contenu test".getBytes());
    }

    // ==========================================================
    // initBucket
    // ==========================================================

    @Test
    void initBucket_bucketDoesNotExist_createsBucket() throws Exception {
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(false);

        minioService.initBucket();

        verify(minioClient).makeBucket(any(MakeBucketArgs.class));
    }

    @Test
    void initBucket_bucketAlreadyExists_doesNotCreateBucket() throws Exception {
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

        minioService.initBucket();

        verify(minioClient, never()).makeBucket(any(MakeBucketArgs.class));
    }

    @Test
    void initBucket_exceptionThrown_isCaughtSilently() throws Exception {
        when(minioClient.bucketExists(any(BucketExistsArgs.class)))
                .thenThrow(new RuntimeException("Erreur Minio"));

        assertDoesNotThrow(() -> minioService.initBucket());

        verify(minioClient, never()).makeBucket(any(MakeBucketArgs.class));
    }

    // ==========================================================
    // uploadDocument
    // ==========================================================

    @Test
    void uploadDocument_success_returnsObjectNameAndCallsInitBucket() throws Exception {
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

        String objectName = minioService.uploadDocument("jean.dupont@test.com", file);

        assertNotNull(objectName);
        assertTrue(objectName.startsWith("jean_dupont_test_com/"));
        assertTrue(objectName.endsWith("_document.pdf"));
        verify(minioClient).bucketExists(any(BucketExistsArgs.class));
        verify(minioClient).putObject(any(PutObjectArgs.class));
    }

    @Test
    void uploadDocument_bucketDoesNotExist_createsBucketThenUploads() throws Exception {
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(false);

        String objectName = minioService.uploadDocument("jean@test.com", file);

        assertNotNull(objectName);
        verify(minioClient).makeBucket(any(MakeBucketArgs.class));
        verify(minioClient).putObject(any(PutObjectArgs.class));
    }

    @Test
    void uploadDocument_putObjectThrowsException_propagates() throws Exception {
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
        when(minioClient.putObject(any(PutObjectArgs.class)))
                .thenThrow(new RuntimeException("Erreur upload"));

        assertThrows(RuntimeException.class,
                () -> minioService.uploadDocument("jean@test.com", file));
    }

    // ==========================================================
    // downloadDocument
    // ==========================================================

    @Test
    void downloadDocument_success_returnsBytes() throws Exception {
        byte[] content = "contenu du fichier".getBytes();
        InputStream stream = new ByteArrayInputStream(content);

        // GetObjectResponse extends FilterInputStream, on ne peut pas le mocker facilement,
        // mais minioClient.getObject retourne un GetObjectResponse qui étend InputStream.
        // On simule via un InputStream simple en castant le retour mocké.
        GetObjectResponse response = mock(GetObjectResponse.class);
        when(response.readAllBytes()).thenReturn(content);
        when(minioClient.getObject(any(GetObjectArgs.class))).thenReturn(response);

        byte[] result = minioService.downloadDocument("path/to/object.pdf");

        assertArrayEquals(content, result);
    }

    @Test
    void downloadDocument_exceptionThrown_propagates() throws Exception {
        when(minioClient.getObject(any(GetObjectArgs.class)))
                .thenThrow(new RuntimeException("Objet introuvable"));

        assertThrows(RuntimeException.class,
                () -> minioService.downloadDocument("path/inexistant.pdf"));
    }

    // ==========================================================
    // deleteDocument
    // ==========================================================

    @Test
    void deleteDocument_success() throws Exception {
        minioService.deleteDocument("path/to/object.pdf");

        verify(minioClient).removeObject(any(RemoveObjectArgs.class));
    }

    @Test
    void deleteDocument_exceptionThrown_propagates() throws Exception {
        doThrow(new RuntimeException("Erreur suppression"))
                .when(minioClient).removeObject(any(RemoveObjectArgs.class));

        assertThrows(RuntimeException.class,
                () -> minioService.deleteDocument("path/to/object.pdf"));
    }

    // ==========================================================
    // getDownloadUrl
    // ==========================================================

    @Test
    void getDownloadUrl_success_returnsUrl() throws Exception {
        when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                .thenReturn("https://minio.example.com/test-bucket/object.pdf?signature=abc");

        String url = minioService.getDownloadUrl("object.pdf");

        assertEquals("https://minio.example.com/test-bucket/object.pdf?signature=abc", url);
    }

    @Test
    void getDownloadUrl_exceptionThrown_propagates() throws Exception {
        when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                .thenThrow(new RuntimeException("Erreur presigned url"));

        assertThrows(RuntimeException.class,
                () -> minioService.getDownloadUrl("object.pdf"));
    }

    // ==========================================================
    // uploadRaw
    // ==========================================================

    @Test
    void uploadRaw_success() throws Exception {
        minioService.uploadRaw("custom/path/object.pdf", file);

        verify(minioClient).putObject(any(PutObjectArgs.class));
    }

    @Test
    void uploadRaw_exceptionThrown_propagates() throws Exception {
        when(minioClient.putObject(any(PutObjectArgs.class)))
                .thenThrow(new RuntimeException("Erreur upload raw"));

        assertThrows(RuntimeException.class,
                () -> minioService.uploadRaw("custom/path/object.pdf", file));
    }
}